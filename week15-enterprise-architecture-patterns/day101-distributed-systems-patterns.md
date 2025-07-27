# Day 101: Distributed Systems Patterns - Event Sourcing, CQRS & Saga Pattern

## Learning Objectives
- Master Event Sourcing pattern for audit trails and temporal data
- Implement Command Query Responsibility Segregation (CQRS) architecture
- Design and implement Saga pattern for distributed transactions
- Understand event-driven architecture patterns and trade-offs
- Build resilient distributed systems with compensation logic

## Topics Covered
1. Event Sourcing Fundamentals and Implementation
2. Command Query Responsibility Segregation (CQRS)
3. Saga Pattern for Distributed Transactions
4. Event Store Design and Implementation
5. Event Streaming and Processing
6. Distributed System Resilience Patterns
7. Real-World Implementation Examples

## 1. Event Sourcing Fundamentals

### Core Concepts

Event Sourcing stores business events as the primary source of truth, allowing reconstruction of current state by replaying events.

### Key Benefits
- Complete audit trail
- Temporal queries
- Natural event-driven architecture
- Debugging and troubleshooting capabilities
- Handling of concurrent modifications

### Event Store Implementation

```java
// Base Event interface
public interface DomainEvent {
    String getEventId();
    String getAggregateId();
    String getEventType();
    Instant getTimestamp();
    long getVersion();
    Object getEventData();
}

// Abstract base event
public abstract class BaseEvent implements DomainEvent {
    private final String eventId;
    private final String aggregateId;
    private final Instant timestamp;
    private final long version;
    
    protected BaseEvent(String aggregateId, long version) {
        this.eventId = UUID.randomUUID().toString();
        this.aggregateId = aggregateId;
        this.timestamp = Instant.now();
        this.version = version;
    }
    
    @Override
    public String getEventId() { return eventId; }
    
    @Override
    public String getAggregateId() { return aggregateId; }
    
    @Override
    public Instant getTimestamp() { return timestamp; }
    
    @Override
    public long getVersion() { return version; }
    
    @Override
    public String getEventType() { return this.getClass().getSimpleName(); }
}

// Trading Domain Events
public class OrderPlacedEvent extends BaseEvent {
    private final String traderId;
    private final String symbol;
    private final BigDecimal quantity;
    private final BigDecimal price;
    private final OrderSide side;
    private final OrderType orderType;
    
    public OrderPlacedEvent(String aggregateId, long version, String traderId, 
                           String symbol, BigDecimal quantity, BigDecimal price,
                           OrderSide side, OrderType orderType) {
        super(aggregateId, version);
        this.traderId = traderId;
        this.symbol = symbol;
        this.quantity = quantity;
        this.price = price;
        this.side = side;
        this.orderType = orderType;
    }
    
    @Override
    public Object getEventData() {
        return Map.of(
                "traderId", traderId,
                "symbol", symbol,
                "quantity", quantity,
                "price", price,
                "side", side.name(),
                "orderType", orderType.name()
        );
    }
    
    // Getters
    public String getTraderId() { return traderId; }
    public String getSymbol() { return symbol; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }
    public OrderSide getSide() { return side; }
    public OrderType getOrderType() { return orderType; }
}

public class OrderCancelledEvent extends BaseEvent {
    private final String reason;
    private final String cancelledBy;
    
    public OrderCancelledEvent(String aggregateId, long version, String reason, 
                              String cancelledBy) {
        super(aggregateId, version);
        this.reason = reason;
        this.cancelledBy = cancelledBy;
    }
    
    @Override
    public Object getEventData() {
        return Map.of(
                "reason", reason,
                "cancelledBy", cancelledBy
        );
    }
    
    public String getReason() { return reason; }
    public String getCancelledBy() { return cancelledBy; }
}

public class OrderExecutedEvent extends BaseEvent {
    private final BigDecimal executedQuantity;
    private final BigDecimal executionPrice;
    private final String executionId;
    private final Instant executionTime;
    
    public OrderExecutedEvent(String aggregateId, long version, BigDecimal executedQuantity,
                             BigDecimal executionPrice, String executionId) {
        super(aggregateId, version);
        this.executedQuantity = executedQuantity;
        this.executionPrice = executionPrice;
        this.executionId = executionId;
        this.executionTime = Instant.now();
    }
    
    @Override
    public Object getEventData() {
        return Map.of(
                "executedQuantity", executedQuantity,
                "executionPrice", executionPrice,
                "executionId", executionId,
                "executionTime", executionTime.toString()
        );
    }
    
    // Getters
    public BigDecimal getExecutedQuantity() { return executedQuantity; }
    public BigDecimal getExecutionPrice() { return executionPrice; }
    public String getExecutionId() { return executionId; }
    public Instant getExecutionTime() { return executionTime; }
}

// Event Store interface
public interface EventStore {
    void saveEvents(String aggregateId, List<DomainEvent> events, long expectedVersion);
    List<DomainEvent> getEvents(String aggregateId);
    List<DomainEvent> getEventsFromVersion(String aggregateId, long fromVersion);
    List<DomainEvent> getAllEvents();
    List<DomainEvent> getEventsByType(String eventType);
    Optional<DomainEvent> getEvent(String eventId);
}

// In-memory Event Store implementation
@Component
public class InMemoryEventStore implements EventStore {
    
    private final Map<String, List<DomainEvent>> eventStreams = new ConcurrentHashMap<>();
    private final Map<String, DomainEvent> eventIndex = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Logger logger = LoggerFactory.getLogger(InMemoryEventStore.class);
    
    @Override
    public void saveEvents(String aggregateId, List<DomainEvent> events, long expectedVersion) {
        lock.writeLock().lock();
        try {
            List<DomainEvent> currentEvents = eventStreams.computeIfAbsent(
                    aggregateId, k -> new ArrayList<>());
            
            // Optimistic concurrency control
            long currentVersion = currentEvents.isEmpty() ? 0 : 
                    currentEvents.get(currentEvents.size() - 1).getVersion();
            
            if (currentVersion != expectedVersion) {
                throw new ConcurrencyException(
                        String.format("Expected version %d but was %d for aggregate %s",
                                expectedVersion, currentVersion, aggregateId));
            }
            
            // Validate event versions
            for (int i = 0; i < events.size(); i++) {
                DomainEvent event = events.get(i);
                long expectedEventVersion = expectedVersion + i + 1;
                if (event.getVersion() != expectedEventVersion) {
                    throw new IllegalArgumentException(
                            String.format("Event version mismatch. Expected %d but was %d",
                                    expectedEventVersion, event.getVersion()));
                }
            }
            
            // Save events
            currentEvents.addAll(events);
            
            // Index events by ID
            events.forEach(event -> eventIndex.put(event.getEventId(), event));
            
            logger.info("Saved {} events for aggregate {}", events.size(), aggregateId);
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public List<DomainEvent> getEvents(String aggregateId) {
        lock.readLock().lock();
        try {
            return new ArrayList<>(eventStreams.getOrDefault(aggregateId, List.of()));
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public List<DomainEvent> getEventsFromVersion(String aggregateId, long fromVersion) {
        lock.readLock().lock();
        try {
            List<DomainEvent> allEvents = eventStreams.getOrDefault(aggregateId, List.of());
            return allEvents.stream()
                    .filter(event -> event.getVersion() > fromVersion)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public List<DomainEvent> getAllEvents() {
        lock.readLock().lock();
        try {
            return eventStreams.values().stream()
                    .flatMap(List::stream)
                    .sorted(Comparator.comparing(DomainEvent::getTimestamp))
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public List<DomainEvent> getEventsByType(String eventType) {
        lock.readLock().lock();
        try {
            return getAllEvents().stream()
                    .filter(event -> event.getEventType().equals(eventType))
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public Optional<DomainEvent> getEvent(String eventId) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(eventIndex.get(eventId));
        } finally {
            lock.readLock().unlock();
        }
    }
}

// Custom exceptions
public class ConcurrencyException extends RuntimeException {
    public ConcurrencyException(String message) {
        super(message);
    }
}
```

### Aggregate Root with Event Sourcing

```java
// Event sourced aggregate root
public abstract class EventSourcedAggregateRoot {
    protected String id;
    protected long version;
    private final List<DomainEvent> uncommittedEvents = new ArrayList<>();
    
    protected EventSourcedAggregateRoot() {
        this.version = 0;
    }
    
    protected EventSourcedAggregateRoot(String id) {
        this.id = id;
        this.version = 0;
    }
    
    public String getId() { return id; }
    public long getVersion() { return version; }
    
    public List<DomainEvent> getUncommittedEvents() {
        return Collections.unmodifiableList(uncommittedEvents);
    }
    
    public void markEventsAsCommitted() {
        uncommittedEvents.clear();
    }
    
    protected void applyEvent(DomainEvent event) {
        handle(event);
        uncommittedEvents.add(event);
    }
    
    public void replayEvents(List<DomainEvent> events) {
        events.forEach(this::handle);
        this.version = events.isEmpty() ? 0 : 
                events.get(events.size() - 1).getVersion();
    }
    
    protected abstract void handle(DomainEvent event);
}

// Trading Order aggregate with event sourcing
public class EventSourcedOrder extends EventSourcedAggregateRoot {
    
    private String traderId;
    private String symbol;
    private BigDecimal quantity;
    private BigDecimal price;
    private OrderSide side;
    private OrderType orderType;
    private OrderStatus status;
    private BigDecimal filledQuantity;
    private List<Execution> executions;
    private Instant createdAt;
    private Instant updatedAt;
    
    // Private constructor for event sourcing
    private EventSourcedOrder() {
        super();
        this.executions = new ArrayList<>();
        this.filledQuantity = BigDecimal.ZERO;
        this.status = OrderStatus.PENDING;
    }
    
    // Factory method for creating new orders
    public static EventSourcedOrder placeOrder(String orderId, String traderId, String symbol,
                                              BigDecimal quantity, BigDecimal price,
                                              OrderSide side, OrderType orderType) {
        EventSourcedOrder order = new EventSourcedOrder();
        order.id = orderId;
        
        OrderPlacedEvent event = new OrderPlacedEvent(orderId, 1, traderId, symbol, 
                quantity, price, side, orderType);
        order.applyEvent(event);
        
        return order;
    }
    
    // Factory method for reconstructing from events
    public static EventSourcedOrder fromEvents(String orderId, List<DomainEvent> events) {
        EventSourcedOrder order = new EventSourcedOrder();
        order.id = orderId;
        order.replayEvents(events);
        return order;
    }
    
    // Business methods
    public void cancel(String reason, String cancelledBy) {
        if (status != OrderStatus.PENDING && status != OrderStatus.PARTIALLY_FILLED) {
            throw new IllegalStateException("Cannot cancel order in status: " + status);
        }
        
        OrderCancelledEvent event = new OrderCancelledEvent(id, version + 1, reason, cancelledBy);
        applyEvent(event);
    }
    
    public void execute(BigDecimal executedQuantity, BigDecimal executionPrice, String executionId) {
        if (status != OrderStatus.PENDING && status != OrderStatus.PARTIALLY_FILLED) {
            throw new IllegalStateException("Cannot execute order in status: " + status);
        }
        
        if (executedQuantity.compareTo(getRemainingQuantity()) > 0) {
            throw new IllegalArgumentException("Execution quantity exceeds remaining quantity");
        }
        
        OrderExecutedEvent event = new OrderExecutedEvent(id, version + 1, 
                executedQuantity, executionPrice, executionId);
        applyEvent(event);
    }
    
    @Override
    protected void handle(DomainEvent event) {
        switch (event) {
            case OrderPlacedEvent e -> handleOrderPlaced(e);
            case OrderCancelledEvent e -> handleOrderCancelled(e);
            case OrderExecutedEvent e -> handleOrderExecuted(e);
            default -> throw new IllegalArgumentException("Unknown event type: " + 
                    event.getClass().getSimpleName());
        }
        this.version = event.getVersion();
        this.updatedAt = event.getTimestamp();
    }
    
    private void handleOrderPlaced(OrderPlacedEvent event) {
        this.traderId = event.getTraderId();
        this.symbol = event.getSymbol();
        this.quantity = event.getQuantity();
        this.price = event.getPrice();
        this.side = event.getSide();
        this.orderType = event.getOrderType();
        this.status = OrderStatus.PENDING;
        this.filledQuantity = BigDecimal.ZERO;
        this.createdAt = event.getTimestamp();
    }
    
    private void handleOrderCancelled(OrderCancelledEvent event) {
        this.status = OrderStatus.CANCELLED;
    }
    
    private void handleOrderExecuted(OrderExecutedEvent event) {
        Execution execution = new Execution(
                event.getExecutionId(),
                event.getExecutedQuantity(),
                event.getExecutionPrice(),
                event.getExecutionTime()
        );
        this.executions.add(execution);
        this.filledQuantity = this.filledQuantity.add(event.getExecutedQuantity());
        
        if (this.filledQuantity.compareTo(this.quantity) >= 0) {
            this.status = OrderStatus.FILLED;
        } else {
            this.status = OrderStatus.PARTIALLY_FILLED;
        }
    }
    
    // Helper methods
    public BigDecimal getRemainingQuantity() {
        return quantity.subtract(filledQuantity);
    }
    
    // Getters
    public String getTraderId() { return traderId; }
    public String getSymbol() { return symbol; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }
    public OrderSide getSide() { return side; }
    public OrderType getOrderType() { return orderType; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getFilledQuantity() { return filledQuantity; }
    public List<Execution> getExecutions() { return Collections.unmodifiableList(executions); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

// Supporting classes
public enum OrderStatus {
    PENDING, PARTIALLY_FILLED, FILLED, CANCELLED, REJECTED
}

public enum OrderSide {
    BUY, SELL
}

public enum OrderType {
    MARKET, LIMIT, STOP, STOP_LIMIT
}

public record Execution(
        String executionId,
        BigDecimal quantity,
        BigDecimal price,
        Instant timestamp
) {}
```

## 2. Command Query Responsibility Segregation (CQRS)

### CQRS Architecture Implementation

```java
// Command side - Write model
public interface Command {
    String getCommandId();
    Instant getTimestamp();
}

public abstract class BaseCommand implements Command {
    private final String commandId;
    private final Instant timestamp;
    
    protected BaseCommand() {
        this.commandId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
    }
    
    @Override
    public String getCommandId() { return commandId; }
    
    @Override
    public Instant getTimestamp() { return timestamp; }
}

// Trading Commands
public class PlaceOrderCommand extends BaseCommand {
    private final String orderId;
    private final String traderId;
    private final String symbol;
    private final BigDecimal quantity;
    private final BigDecimal price;
    private final OrderSide side;
    private final OrderType orderType;
    
    public PlaceOrderCommand(String orderId, String traderId, String symbol,
                            BigDecimal quantity, BigDecimal price, 
                            OrderSide side, OrderType orderType) {
        super();
        this.orderId = orderId;
        this.traderId = traderId;
        this.symbol = symbol;
        this.quantity = quantity;
        this.price = price;
        this.side = side;
        this.orderType = orderType;
    }
    
    // Getters
    public String getOrderId() { return orderId; }
    public String getTraderId() { return traderId; }
    public String getSymbol() { return symbol; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }
    public OrderSide getSide() { return side; }
    public OrderType getOrderType() { return orderType; }
}

public class CancelOrderCommand extends BaseCommand {
    private final String orderId;
    private final String reason;
    private final String cancelledBy;
    
    public CancelOrderCommand(String orderId, String reason, String cancelledBy) {
        super();
        this.orderId = orderId;
        this.reason = reason;
        this.cancelledBy = cancelledBy;
    }
    
    public String getOrderId() { return orderId; }
    public String getReason() { return reason; }
    public String getCancelledBy() { return cancelledBy; }
}

// Command Handler interface
public interface CommandHandler<T extends Command> {
    void handle(T command);
}

// Command Bus
@Component
public class CommandBus {
    
    private final Map<Class<? extends Command>, CommandHandler<? extends Command>> handlers;
    private final Logger logger = LoggerFactory.getLogger(CommandBus.class);
    
    public CommandBus(List<CommandHandler<? extends Command>> commandHandlers) {
        this.handlers = new HashMap<>();
        registerHandlers(commandHandlers);
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Command> void send(T command) {
        CommandHandler<T> handler = (CommandHandler<T>) handlers.get(command.getClass());
        if (handler == null) {
            throw new IllegalArgumentException("No handler found for command: " + 
                    command.getClass().getSimpleName());
        }
        
        logger.info("Handling command: {} with ID: {}", 
                command.getClass().getSimpleName(), command.getCommandId());
        
        try {
            handler.handle(command);
            logger.info("Successfully handled command: {}", command.getCommandId());
        } catch (Exception e) {
            logger.error("Error handling command: {}", command.getCommandId(), e);
            throw new CommandHandlingException("Failed to handle command", e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void registerHandlers(List<CommandHandler<? extends Command>> commandHandlers) {
        for (CommandHandler<? extends Command> handler : commandHandlers) {
            Type[] genericInterfaces = handler.getClass().getGenericInterfaces();
            for (Type genericInterface : genericInterfaces) {
                if (genericInterface instanceof ParameterizedType parameterizedType) {
                    if (parameterizedType.getRawType().equals(CommandHandler.class)) {
                        Type commandType = parameterizedType.getActualTypeArguments()[0];
                        Class<? extends Command> commandClass = 
                                (Class<? extends Command>) commandType;
                        handlers.put(commandClass, handler);
                        logger.info("Registered command handler for: {}", 
                                commandClass.getSimpleName());
                        break;
                    }
                }
            }
        }
    }
}

// Order Command Handlers
@Component
public class PlaceOrderCommandHandler implements CommandHandler<PlaceOrderCommand> {
    
    private final EventStore eventStore;
    private final Logger logger = LoggerFactory.getLogger(PlaceOrderCommandHandler.class);
    
    public PlaceOrderCommandHandler(EventStore eventStore) {
        this.eventStore = eventStore;
    }
    
    @Override
    public void handle(PlaceOrderCommand command) {
        logger.info("Placing order: {}", command.getOrderId());
        
        // Validate command
        validateCommand(command);
        
        // Create new order aggregate
        EventSourcedOrder order = EventSourcedOrder.placeOrder(
                command.getOrderId(),
                command.getTraderId(),
                command.getSymbol(),
                command.getQuantity(),
                command.getPrice(),
                command.getSide(),
                command.getOrderType()
        );
        
        // Save events
        eventStore.saveEvents(order.getId(), order.getUncommittedEvents(), 0);
        order.markEventsAsCommitted();
        
        logger.info("Order placed successfully: {}", command.getOrderId());
    }
    
    private void validateCommand(PlaceOrderCommand command) {
        if (command.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Order quantity must be positive");
        }
        if (command.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Order price must be positive");
        }
        if (command.getSymbol() == null || command.getSymbol().trim().isEmpty()) {
            throw new IllegalArgumentException("Order symbol cannot be empty");
        }
        if (command.getTraderId() == null || command.getTraderId().trim().isEmpty()) {
            throw new IllegalArgumentException("Trader ID cannot be empty");
        }
    }
}

@Component
public class CancelOrderCommandHandler implements CommandHandler<CancelOrderCommand> {
    
    private final EventStore eventStore;
    private final Logger logger = LoggerFactory.getLogger(CancelOrderCommandHandler.class);
    
    public CancelOrderCommandHandler(EventStore eventStore) {
        this.eventStore = eventStore;
    }
    
    @Override
    public void handle(CancelOrderCommand command) {
        logger.info("Cancelling order: {}", command.getOrderId());
        
        // Load existing order
        List<DomainEvent> events = eventStore.getEvents(command.getOrderId());
        if (events.isEmpty()) {
            throw new IllegalArgumentException("Order not found: " + command.getOrderId());
        }
        
        EventSourcedOrder order = EventSourcedOrder.fromEvents(command.getOrderId(), events);
        
        // Cancel order
        order.cancel(command.getReason(), command.getCancelledBy());
        
        // Save events
        eventStore.saveEvents(order.getId(), order.getUncommittedEvents(), order.getVersion());
        order.markEventsAsCommitted();
        
        logger.info("Order cancelled successfully: {}", command.getOrderId());
    }
}

// Query side - Read model
public class OrderView {
    private String orderId;
    private String traderId;
    private String symbol;
    private BigDecimal quantity;
    private BigDecimal price;
    private OrderSide side;
    private OrderType orderType;
    private OrderStatus status;
    private BigDecimal filledQuantity;
    private List<ExecutionView> executions;
    private Instant createdAt;
    private Instant updatedAt;
    
    // Constructors
    public OrderView() {
        this.executions = new ArrayList<>();
    }
    
    public OrderView(String orderId, String traderId, String symbol, BigDecimal quantity,
                    BigDecimal price, OrderSide side, OrderType orderType, OrderStatus status,
                    BigDecimal filledQuantity, Instant createdAt, Instant updatedAt) {
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
        this.updatedAt = updatedAt;
        this.executions = new ArrayList<>();
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
    
    public OrderSide getSide() { return side; }
    public void setSide(OrderSide side) { this.side = side; }
    
    public OrderType getOrderType() { return orderType; }
    public void setOrderType(OrderType orderType) { this.orderType = orderType; }
    
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    
    public BigDecimal getFilledQuantity() { return filledQuantity; }
    public void setFilledQuantity(BigDecimal filledQuantity) { this.filledQuantity = filledQuantity; }
    
    public List<ExecutionView> getExecutions() { return executions; }
    public void setExecutions(List<ExecutionView> executions) { this.executions = executions; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

public class ExecutionView {
    private String executionId;
    private String orderId;
    private BigDecimal quantity;
    private BigDecimal price;
    private Instant timestamp;
    
    public ExecutionView() {}
    
    public ExecutionView(String executionId, String orderId, BigDecimal quantity,
                        BigDecimal price, Instant timestamp) {
        this.executionId = executionId;
        this.orderId = orderId;
        this.quantity = quantity;
        this.price = price;
        this.timestamp = timestamp;
    }
    
    // Getters and setters
    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }
    
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}

// Query interface
public interface Query {
    String getQueryId();
}

// Order Queries
public class GetOrderByIdQuery implements Query {
    private final String queryId;
    private final String orderId;
    
    public GetOrderByIdQuery(String orderId) {
        this.queryId = UUID.randomUUID().toString();
        this.orderId = orderId;
    }
    
    @Override
    public String getQueryId() { return queryId; }
    public String getOrderId() { return orderId; }
}

public class GetOrdersByTraderQuery implements Query {
    private final String queryId;
    private final String traderId;
    private final Optional<OrderStatus> status;
    private final Optional<String> symbol;
    
    public GetOrdersByTraderQuery(String traderId, Optional<OrderStatus> status, 
                                 Optional<String> symbol) {
        this.queryId = UUID.randomUUID().toString();
        this.traderId = traderId;
        this.status = status;
        this.symbol = symbol;
    }
    
    @Override
    public String getQueryId() { return queryId; }
    public String getTraderId() { return traderId; }
    public Optional<OrderStatus> getStatus() { return status; }
    public Optional<String> getSymbol() { return symbol; }
}

// Query Handler interface
public interface QueryHandler<T extends Query, R> {
    R handle(T query);
}

// Read Model Repository
public interface OrderViewRepository {
    Optional<OrderView> findById(String orderId);
    List<OrderView> findByTraderId(String traderId);
    List<OrderView> findByTraderIdAndStatus(String traderId, OrderStatus status);
    List<OrderView> findByTraderIdAndSymbol(String traderId, String symbol);
    List<OrderView> findBySymbol(String symbol);
    void save(OrderView orderView);
    void delete(String orderId);
}

@Repository
public class InMemoryOrderViewRepository implements OrderViewRepository {
    
    private final Map<String, OrderView> orders = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    @Override
    public Optional<OrderView> findById(String orderId) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(orders.get(orderId));
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public List<OrderView> findByTraderId(String traderId) {
        lock.readLock().lock();
        try {
            return orders.values().stream()
                    .filter(order -> order.getTraderId().equals(traderId))
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public List<OrderView> findByTraderIdAndStatus(String traderId, OrderStatus status) {
        lock.readLock().lock();
        try {
            return orders.values().stream()
                    .filter(order -> order.getTraderId().equals(traderId) && 
                            order.getStatus() == status)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public List<OrderView> findByTraderIdAndSymbol(String traderId, String symbol) {
        lock.readLock().lock();
        try {
            return orders.values().stream()
                    .filter(order -> order.getTraderId().equals(traderId) && 
                            order.getSymbol().equals(symbol))
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public List<OrderView> findBySymbol(String symbol) {
        lock.readLock().lock();
        try {
            return orders.values().stream()
                    .filter(order -> order.getSymbol().equals(symbol))
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public void save(OrderView orderView) {
        lock.writeLock().lock();
        try {
            orders.put(orderView.getOrderId(), orderView);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public void delete(String orderId) {
        lock.writeLock().lock();
        try {
            orders.remove(orderId);
        } finally {
            lock.writeLock().unlock();
        }
    }
}

// Query Handlers
@Component
public class GetOrderByIdQueryHandler implements QueryHandler<GetOrderByIdQuery, Optional<OrderView>> {
    
    private final OrderViewRepository repository;
    
    public GetOrderByIdQueryHandler(OrderViewRepository repository) {
        this.repository = repository;
    }
    
    @Override
    public Optional<OrderView> handle(GetOrderByIdQuery query) {
        return repository.findById(query.getOrderId());
    }
}

@Component
public class GetOrdersByTraderQueryHandler implements QueryHandler<GetOrdersByTraderQuery, List<OrderView>> {
    
    private final OrderViewRepository repository;
    
    public GetOrdersByTraderQueryHandler(OrderViewRepository repository) {
        this.repository = repository;
    }
    
    @Override
    public List<OrderView> handle(GetOrdersByTraderQuery query) {
        if (query.getStatus().isPresent() && query.getSymbol().isPresent()) {
            return repository.findByTraderIdAndSymbol(query.getTraderId(), query.getSymbol().get())
                    .stream()
                    .filter(order -> order.getStatus() == query.getStatus().get())
                    .collect(Collectors.toList());
        } else if (query.getStatus().isPresent()) {
            return repository.findByTraderIdAndStatus(query.getTraderId(), query.getStatus().get());
        } else if (query.getSymbol().isPresent()) {
            return repository.findByTraderIdAndSymbol(query.getTraderId(), query.getSymbol().get());
        } else {
            return repository.findByTraderId(query.getTraderId());
        }
    }
}

// Custom exceptions
public class CommandHandlingException extends RuntimeException {
    public CommandHandlingException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

## 3. Saga Pattern for Distributed Transactions

### Saga Implementation

```java
// Saga interface
public interface Saga {
    String getSagaId();
    String getSagaType();
    SagaStatus getStatus();
    List<SagaStep> getSteps();
    void addStep(SagaStep step);
    void executeNext();
    void compensate();
}

// Saga Status
public enum SagaStatus {
    PENDING, IN_PROGRESS, COMPLETED, FAILED, COMPENSATING, COMPENSATED
}

// Saga Step
public interface SagaStep {
    String getStepId();
    String getStepType();
    StepStatus getStatus();
    void execute();
    void compensate();
    boolean canCompensate();
}

public enum StepStatus {
    PENDING, COMPLETED, FAILED, COMPENSATED
}

// Base Saga implementation
public abstract class BaseSaga implements Saga {
    protected final String sagaId;
    protected final String sagaType;
    protected SagaStatus status;
    protected final List<SagaStep> steps;
    protected int currentStepIndex;
    
    protected BaseSaga(String sagaType) {
        this.sagaId = UUID.randomUUID().toString();
        this.sagaType = sagaType;
        this.status = SagaStatus.PENDING;
        this.steps = new ArrayList<>();
        this.currentStepIndex = 0;
    }
    
    @Override
    public String getSagaId() { return sagaId; }
    
    @Override
    public String getSagaType() { return sagaType; }
    
    @Override
    public SagaStatus getStatus() { return status; }
    
    @Override
    public List<SagaStep> getSteps() { return Collections.unmodifiableList(steps); }
    
    @Override
    public void addStep(SagaStep step) {
        if (status != SagaStatus.PENDING) {
            throw new IllegalStateException("Cannot add steps to saga in status: " + status);
        }
        steps.add(step);
    }
    
    @Override
    public void executeNext() {
        if (currentStepIndex >= steps.size()) {
            status = SagaStatus.COMPLETED;
            return;
        }
        
        status = SagaStatus.IN_PROGRESS;
        SagaStep currentStep = steps.get(currentStepIndex);
        
        try {
            currentStep.execute();
            currentStepIndex++;
            
            if (currentStepIndex >= steps.size()) {
                status = SagaStatus.COMPLETED;
            }
        } catch (Exception e) {
            status = SagaStatus.FAILED;
            throw new SagaExecutionException("Saga step failed: " + currentStep.getStepType(), e);
        }
    }
    
    @Override
    public void compensate() {
        status = SagaStatus.COMPENSATING;
        
        // Compensate in reverse order
        for (int i = currentStepIndex - 1; i >= 0; i--) {
            SagaStep step = steps.get(i);
            if (step.getStatus() == StepStatus.COMPLETED && step.canCompensate()) {
                try {
                    step.compensate();
                } catch (Exception e) {
                    // Log error but continue compensation
                    System.err.println("Failed to compensate step: " + step.getStepType() + 
                            " - " + e.getMessage());
                }
            }
        }
        
        status = SagaStatus.COMPENSATED;
    }
}

// Trading Saga for complex order processing
public class OrderProcessingSaga extends BaseSaga {
    
    private final String orderId;
    private final String traderId;
    private final BigDecimal amount;
    
    public OrderProcessingSaga(String orderId, String traderId, BigDecimal amount) {
        super("OrderProcessing");
        this.orderId = orderId;
        this.traderId = traderId;
        this.amount = amount;
        
        // Define saga steps
        addStep(new ValidateTraderStep(traderId));
        addStep(new ReserveFundsStep(traderId, amount));
        addStep(new PlaceOrderStep(orderId));
        addStep(new NotifyTraderStep(traderId, orderId));
    }
    
    public String getOrderId() { return orderId; }
    public String getTraderId() { return traderId; }
    public BigDecimal getAmount() { return amount; }
}

// Saga Steps Implementation
public class ValidateTraderStep implements SagaStep {
    private final String stepId;
    private final String traderId;
    private StepStatus status;
    
    public ValidateTraderStep(String traderId) {
        this.stepId = UUID.randomUUID().toString();
        this.traderId = traderId;
        this.status = StepStatus.PENDING;
    }
    
    @Override
    public String getStepId() { return stepId; }
    
    @Override
    public String getStepType() { return "ValidateTrader"; }
    
    @Override
    public StepStatus getStatus() { return status; }
    
    @Override
    public void execute() {
        try {
            // Simulate trader validation
            if (traderId == null || traderId.trim().isEmpty()) {
                throw new IllegalArgumentException("Invalid trader ID");
            }
            
            // Simulate validation logic
            Thread.sleep(100);
            
            status = StepStatus.COMPLETED;
            System.out.println("Trader validated: " + traderId);
            
        } catch (Exception e) {
            status = StepStatus.FAILED;
            throw new RuntimeException("Trader validation failed", e);
        }
    }
    
    @Override
    public void compensate() {
        // No compensation needed for validation
        status = StepStatus.COMPENSATED;
        System.out.println("Trader validation compensation (no-op): " + traderId);
    }
    
    @Override
    public boolean canCompensate() {
        return status == StepStatus.COMPLETED;
    }
}

public class ReserveFundsStep implements SagaStep {
    private final String stepId;
    private final String traderId;
    private final BigDecimal amount;
    private StepStatus status;
    private String reservationId;
    
    public ReserveFundsStep(String traderId, BigDecimal amount) {
        this.stepId = UUID.randomUUID().toString();
        this.traderId = traderId;
        this.amount = amount;
        this.status = StepStatus.PENDING;
    }
    
    @Override
    public String getStepId() { return stepId; }
    
    @Override
    public String getStepType() { return "ReserveFunds"; }
    
    @Override
    public StepStatus getStatus() { return status; }
    
    @Override
    public void execute() {
        try {
            // Simulate funds reservation
            this.reservationId = UUID.randomUUID().toString();
            
            // Simulate reservation logic
            Thread.sleep(150);
            
            status = StepStatus.COMPLETED;
            System.out.println("Funds reserved: " + amount + " for trader: " + traderId + 
                    " (reservation: " + reservationId + ")");
            
        } catch (Exception e) {
            status = StepStatus.FAILED;
            throw new RuntimeException("Funds reservation failed", e);
        }
    }
    
    @Override
    public void compensate() {
        try {
            // Release reserved funds
            if (reservationId != null) {
                // Simulate fund release
                Thread.sleep(100);
                status = StepStatus.COMPENSATED;
                System.out.println("Funds released: " + amount + " for trader: " + traderId + 
                        " (reservation: " + reservationId + ")");
            }
        } catch (Exception e) {
            throw new RuntimeException("Fund release compensation failed", e);
        }
    }
    
    @Override
    public boolean canCompensate() {
        return status == StepStatus.COMPLETED && reservationId != null;
    }
}

public class PlaceOrderStep implements SagaStep {
    private final String stepId;
    private final String orderId;
    private StepStatus status;
    
    public PlaceOrderStep(String orderId) {
        this.stepId = UUID.randomUUID().toString();
        this.orderId = orderId;
        this.status = StepStatus.PENDING;
    }
    
    @Override
    public String getStepId() { return stepId; }
    
    @Override
    public String getStepType() { return "PlaceOrder"; }
    
    @Override
    public StepStatus getStatus() { return status; }
    
    @Override
    public void execute() {
        try {
            // Simulate order placement
            Thread.sleep(200);
            
            status = StepStatus.COMPLETED;
            System.out.println("Order placed: " + orderId);
            
        } catch (Exception e) {
            status = StepStatus.FAILED;
            throw new RuntimeException("Order placement failed", e);
        }
    }
    
    @Override
    public void compensate() {
        try {
            // Cancel the placed order
            Thread.sleep(100);
            status = StepStatus.COMPENSATED;
            System.out.println("Order cancelled: " + orderId);
            
        } catch (Exception e) {
            throw new RuntimeException("Order cancellation compensation failed", e);
        }
    }
    
    @Override
    public boolean canCompensate() {
        return status == StepStatus.COMPLETED;
    }
}

public class NotifyTraderStep implements SagaStep {
    private final String stepId;
    private final String traderId;
    private final String orderId;
    private StepStatus status;
    
    public NotifyTraderStep(String traderId, String orderId) {
        this.stepId = UUID.randomUUID().toString();
        this.traderId = traderId;
        this.orderId = orderId;
        this.status = StepStatus.PENDING;
    }
    
    @Override
    public String getStepId() { return stepId; }
    
    @Override
    public String getStepType() { return "NotifyTrader"; }
    
    @Override
    public StepStatus getStatus() { return status; }
    
    @Override
    public void execute() {
        try {
            // Simulate trader notification
            Thread.sleep(50);
            
            status = StepStatus.COMPLETED;
            System.out.println("Trader notified: " + traderId + " about order: " + orderId);
            
        } catch (Exception e) {
            status = StepStatus.FAILED;
            throw new RuntimeException("Trader notification failed", e);
        }
    }
    
    @Override
    public void compensate() {
        try {
            // Send cancellation notification
            Thread.sleep(50);
            status = StepStatus.COMPENSATED;
            System.out.println("Cancellation notification sent to trader: " + traderId + 
                    " for order: " + orderId);
            
        } catch (Exception e) {
            throw new RuntimeException("Cancellation notification compensation failed", e);
        }
    }
    
    @Override
    public boolean canCompensate() {
        return status == StepStatus.COMPLETED;
    }
}

// Saga Manager
@Component
public class SagaManager {
    
    private final Map<String, Saga> activeSagas = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final Logger logger = LoggerFactory.getLogger(SagaManager.class);
    
    public void startSaga(Saga saga) {
        activeSagas.put(saga.getSagaId(), saga);
        
        executorService.submit(() -> {
            try {
                executeSaga(saga);
            } catch (Exception e) {
                logger.error("Saga execution failed: {}", saga.getSagaId(), e);
                compensateSaga(saga);
            } finally {
                activeSagas.remove(saga.getSagaId());
            }
        });
    }
    
    private void executeSaga(Saga saga) {
        logger.info("Starting saga execution: {}", saga.getSagaId());
        
        while (saga.getStatus() == SagaStatus.PENDING || saga.getStatus() == SagaStatus.IN_PROGRESS) {
            saga.executeNext();
        }
        
        if (saga.getStatus() == SagaStatus.COMPLETED) {
            logger.info("Saga completed successfully: {}", saga.getSagaId());
        } else {
            logger.error("Saga failed: {}", saga.getSagaId());
            throw new SagaExecutionException("Saga execution failed");
        }
    }
    
    private void compensateSaga(Saga saga) {
        logger.info("Starting saga compensation: {}", saga.getSagaId());
        
        try {
            saga.compensate();
            logger.info("Saga compensation completed: {}", saga.getSagaId());
        } catch (Exception e) {
            logger.error("Saga compensation failed: {}", saga.getSagaId(), e);
        }
    }
    
    public Optional<Saga> getSaga(String sagaId) {
        return Optional.ofNullable(activeSagas.get(sagaId));
    }
    
    public List<Saga> getActiveSagas() {
        return new ArrayList<>(activeSagas.values());
    }
    
    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

// Custom exceptions
public class SagaExecutionException extends RuntimeException {
    public SagaExecutionException(String message) {
        super(message);
    }
    
    public SagaExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

## 4. Integration and Usage Examples

### REST Controller with CQRS

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    private final CommandBus commandBus;
    private final GetOrderByIdQueryHandler getOrderByIdHandler;
    private final GetOrdersByTraderQueryHandler getOrdersByTraderHandler;
    private final SagaManager sagaManager;
    
    public OrderController(CommandBus commandBus, 
                          GetOrderByIdQueryHandler getOrderByIdHandler,
                          GetOrdersByTraderQueryHandler getOrdersByTraderHandler,
                          SagaManager sagaManager) {
        this.commandBus = commandBus;
        this.getOrderByIdHandler = getOrderByIdHandler;
        this.getOrdersByTraderHandler = getOrdersByTraderHandler;
        this.sagaManager = sagaManager;
    }
    
    @PostMapping
    public ResponseEntity<Map<String, String>> placeOrder(@RequestBody PlaceOrderRequest request) {
        String orderId = UUID.randomUUID().toString();
        
        PlaceOrderCommand command = new PlaceOrderCommand(
                orderId, request.getTraderId(), request.getSymbol(),
                request.getQuantity(), request.getPrice(),
                request.getSide(), request.getOrderType()
        );
        
        // Option 1: Simple command processing
        commandBus.send(command);
        
        // Option 2: Using Saga for complex processing
        OrderProcessingSaga saga = new OrderProcessingSaga(
                orderId, request.getTraderId(), 
                request.getQuantity().multiply(request.getPrice())
        );
        sagaManager.startSaga(saga);
        
        return ResponseEntity.accepted()
                .body(Map.of("orderId", orderId, "sagaId", saga.getSagaId()));
    }
    
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> cancelOrder(@PathVariable String orderId, 
                                          @RequestParam String reason,
                                          @RequestParam String cancelledBy) {
        CancelOrderCommand command = new CancelOrderCommand(orderId, reason, cancelledBy);
        commandBus.send(command);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderView> getOrder(@PathVariable String orderId) {
        GetOrderByIdQuery query = new GetOrderByIdQuery(orderId);
        Optional<OrderView> order = getOrderByIdHandler.handle(query);
        
        return order.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    public ResponseEntity<List<OrderView>> getOrders(@RequestParam String traderId,
                                                    @RequestParam(required = false) OrderStatus status,
                                                    @RequestParam(required = false) String symbol) {
        GetOrdersByTraderQuery query = new GetOrdersByTraderQuery(
                traderId, Optional.ofNullable(status), Optional.ofNullable(symbol)
        );
        
        List<OrderView> orders = getOrdersByTraderHandler.handle(query);
        return ResponseEntity.ok(orders);
    }
}

// DTOs
public class PlaceOrderRequest {
    private String traderId;
    private String symbol;
    private BigDecimal quantity;
    private BigDecimal price;
    private OrderSide side;
    private OrderType orderType;
    
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
    
    public OrderType getOrderType() { return orderType; }
    public void setOrderType(OrderType orderType) { this.orderType = orderType; }
}
```

## Key Takeaways

1. **Event Sourcing**: Provides complete audit trail and temporal queries at the cost of complexity
2. **CQRS**: Separates read and write models for optimized performance and scalability
3. **Saga Pattern**: Manages distributed transactions with compensation logic for failures
4. **Event Store**: Central component for storing and retrieving domain events
5. **Read Models**: Optimized views for specific query patterns and performance requirements
6. **Resilience**: Compensation logic ensures system consistency even during failures
7. **Scalability**: Asynchronous processing and separate read/write models improve performance

These patterns enable building highly scalable, resilient distributed systems that maintain consistency across service boundaries while providing excellent query performance and complete auditability.