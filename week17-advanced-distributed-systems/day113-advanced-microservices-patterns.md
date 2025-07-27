# Day 113: Advanced Microservices Patterns - Event Sourcing, CQRS & Saga Orchestration

## Learning Objectives
- Master Event Sourcing architecture and implementation patterns
- Implement Command Query Responsibility Segregation (CQRS) with advanced projections
- Design and orchestrate distributed sagas for complex business transactions
- Build event stores with advanced querying and optimization capabilities
- Create comprehensive microservices communication patterns
- Implement advanced event-driven architectures with eventual consistency

## Part 1: Event Sourcing Architecture

### Event Store Implementation with Advanced Features

```java
package com.distributed.eventsourcing.store;

import org.springframework.stereotype.Component;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AdvancedEventStore {

    private static final Logger logger = LoggerFactory.getLogger(AdvancedEventStore.class);

    private final DatabaseClient databaseClient;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final EventSerializer eventSerializer;
    private final SnapshotManager snapshotManager;
    private final EventStreamCache streamCache;

    // In-memory aggregate cache for performance
    private final Map<String, AggregateSnapshot> aggregateCache = new ConcurrentHashMap<>();

    public AdvancedEventStore(DatabaseClient databaseClient, 
            ReactiveRedisTemplate<String, Object> redisTemplate,
            EventSerializer eventSerializer,
            SnapshotManager snapshotManager) {
        this.databaseClient = databaseClient;
        this.redisTemplate = redisTemplate;
        this.eventSerializer = eventSerializer;
        this.snapshotManager = snapshotManager;
        this.streamCache = new EventStreamCache(redisTemplate);
    }

    public Mono<Long> appendEvents(String aggregateId, List<DomainEvent> events, 
            long expectedVersion) {
        
        return validateExpectedVersion(aggregateId, expectedVersion)
                .flatMap(currentVersion -> {
                    List<EventRecord> eventRecords = events.stream()
                            .map(event -> createEventRecord(aggregateId, event, currentVersion))
                            .toList();
                    
                    return saveEventsAtomically(eventRecords)
                            .flatMap(savedEvents -> publishEvents(savedEvents))
                            .flatMap(publishedEvents -> updateCache(aggregateId, publishedEvents))
                            .flatMap(cachedEvents -> checkAndCreateSnapshot(aggregateId))
                            .thenReturn(currentVersion + events.size());
                })
                .doOnSuccess(newVersion -> 
                    logger.info("Successfully appended {} events to aggregate {}, new version: {}", 
                            events.size(), aggregateId, newVersion))
                .onErrorMap(OptimisticLockException.class, 
                    ex -> new ConcurrencyException("Concurrent modification detected for aggregate: " + aggregateId));
    }

    public Flux<DomainEvent> getEventStream(String aggregateId, long fromVersion) {
        return getFromCacheOrDatabase(aggregateId, fromVersion)
                .map(this::deserializeEvent)
                .doOnNext(event -> logger.debug("Retrieved event: {} for aggregate: {}", 
                        event.getClass().getSimpleName(), aggregateId));
    }

    public Flux<DomainEvent> getEventStreamWithProjection(String aggregateId, 
            long fromVersion, EventProjection projection) {
        
        return getEventStream(aggregateId, fromVersion)
                .transform(projection::apply)
                .doOnNext(event -> logger.debug("Applied projection to event: {}", 
                        event.getClass().getSimpleName()));
    }

    public Mono<AggregateRoot> rebuildAggregate(String aggregateId, Class<? extends AggregateRoot> aggregateType) {
        return getLatestSnapshot(aggregateId)
                .switchIfEmpty(createEmptyAggregate(aggregateType))
                .flatMap(snapshot -> {
                    long fromVersion = snapshot.getVersion() + 1;
                    return getEventStream(aggregateId, fromVersion)
                            .reduce(snapshot.getAggregate(), (aggregate, event) -> {
                                aggregate.apply(event);
                                return aggregate;
                            });
                })
                .cast(AggregateRoot.class)
                .doOnSuccess(aggregate -> 
                    logger.info("Rebuilt aggregate {} from {} events", aggregateId, aggregate.getVersion()));
    }

    public Flux<DomainEvent> getAllEventsFromTimestamp(LocalDateTime from, String eventType) {
        String sql = """
                SELECT aggregate_id, event_data, event_type, version, timestamp, metadata
                FROM event_store 
                WHERE timestamp >= $1 
                AND ($2 IS NULL OR event_type = $2)
                ORDER BY timestamp ASC
                """;

        return databaseClient.sql(sql)
                .bind("$1", from)
                .bind("$2", eventType)
                .fetch()
                .all()
                .map(this::mapToEventRecord)
                .map(this::deserializeEvent)
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<EventStreamStatistics> getStreamStatistics(String aggregateId) {
        return Mono.zip(
                getEventCount(aggregateId),
                getFirstEventTimestamp(aggregateId),
                getLastEventTimestamp(aggregateId),
                getEventTypes(aggregateId)
        ).map(tuple -> EventStreamStatistics.builder()
                .aggregateId(aggregateId)
                .eventCount(tuple.getT1())
                .firstEventTimestamp(tuple.getT2())
                .lastEventTimestamp(tuple.getT3())
                .eventTypes(tuple.getT4())
                .build());
    }

    private Mono<Long> validateExpectedVersion(String aggregateId, long expectedVersion) {
        return getCurrentVersion(aggregateId)
                .map(currentVersion -> {
                    if (currentVersion != expectedVersion) {
                        throw new OptimisticLockException(
                                String.format("Expected version %d but current version is %d for aggregate %s",
                                        expectedVersion, currentVersion, aggregateId));
                    }
                    return currentVersion;
                });
    }

    private Mono<Long> getCurrentVersion(String aggregateId) {
        return streamCache.getLatestVersion(aggregateId)
                .switchIfEmpty(
                    databaseClient.sql("SELECT COALESCE(MAX(version), 0) FROM event_store WHERE aggregate_id = $1")
                            .bind("$1", aggregateId)
                            .fetch()
                            .one()
                            .map(row -> (Long) row.get("coalesce"))
                            .defaultIfEmpty(0L)
                );
    }

    private EventRecord createEventRecord(String aggregateId, DomainEvent event, long baseVersion) {
        return EventRecord.builder()
                .aggregateId(aggregateId)
                .eventId(UUID.randomUUID().toString())
                .eventType(event.getClass().getSimpleName())
                .eventData(eventSerializer.serialize(event))
                .version(baseVersion + 1)
                .timestamp(LocalDateTime.now())
                .metadata(createMetadata(event))
                .build();
    }

    private Mono<List<EventRecord>> saveEventsAtomically(List<EventRecord> eventRecords) {
        return databaseClient.inTransaction(transactionalClient -> {
            return Flux.fromIterable(eventRecords)
                    .flatMap(record -> saveEventRecord(transactionalClient, record))
                    .collectList();
        });
    }

    private Mono<EventRecord> saveEventRecord(DatabaseClient client, EventRecord record) {
        String sql = """
                INSERT INTO event_store (aggregate_id, event_id, event_type, event_data, 
                                       version, timestamp, metadata)
                VALUES ($1, $2, $3, $4, $5, $6, $7)
                """;

        return client.sql(sql)
                .bind("$1", record.getAggregateId())
                .bind("$2", record.getEventId())
                .bind("$3", record.getEventType())
                .bind("$4", record.getEventData())
                .bind("$5", record.getVersion())
                .bind("$6", record.getTimestamp())
                .bind("$7", eventSerializer.serialize(record.getMetadata()))
                .fetch()
                .rowsUpdated()
                .thenReturn(record);
    }

    private Mono<List<EventRecord>> publishEvents(List<EventRecord> events) {
        return Flux.fromIterable(events)
                .flatMap(this::publishEvent)
                .collectList();
    }

    private Mono<EventRecord> publishEvent(EventRecord eventRecord) {
        String topic = "domain-events." + eventRecord.getEventType().toLowerCase();
        
        return redisTemplate.convertAndSend(topic, eventRecord)
                .thenReturn(eventRecord)
                .doOnSuccess(published -> 
                    logger.debug("Published event {} to topic {}", eventRecord.getEventId(), topic));
    }

    private Mono<List<EventRecord>> updateCache(String aggregateId, List<EventRecord> events) {
        return streamCache.cacheEvents(aggregateId, events)
                .thenReturn(events);
    }

    private Mono<Void> checkAndCreateSnapshot(String aggregateId) {
        return getCurrentVersion(aggregateId)
                .filter(version -> version % 100 == 0) // Snapshot every 100 events
                .flatMap(version -> createSnapshot(aggregateId))
                .then();
    }

    private Mono<Void> createSnapshot(String aggregateId) {
        return rebuildAggregate(aggregateId, AggregateRoot.class)
                .flatMap(aggregate -> snapshotManager.createSnapshot(aggregateId, aggregate))
                .doOnSuccess(snapshot -> 
                    logger.info("Created snapshot for aggregate {} at version {}", 
                            aggregateId, snapshot.getVersion()));
    }

    private Flux<EventRecord> getFromCacheOrDatabase(String aggregateId, long fromVersion) {
        return streamCache.getEvents(aggregateId, fromVersion)
                .switchIfEmpty(getEventsFromDatabase(aggregateId, fromVersion));
    }

    private Flux<EventRecord> getEventsFromDatabase(String aggregateId, long fromVersion) {
        String sql = """
                SELECT aggregate_id, event_id, event_type, event_data, version, timestamp, metadata
                FROM event_store 
                WHERE aggregate_id = $1 AND version >= $2
                ORDER BY version ASC
                """;

        return databaseClient.sql(sql)
                .bind("$1", aggregateId)
                .bind("$2", fromVersion)
                .fetch()
                .all()
                .map(this::mapToEventRecord);
    }

    private DomainEvent deserializeEvent(EventRecord record) {
        return eventSerializer.deserialize(record.getEventData(), record.getEventType());
    }

    private EventRecord mapToEventRecord(Map<String, Object> row) {
        return EventRecord.builder()
                .aggregateId((String) row.get("aggregate_id"))
                .eventId((String) row.get("event_id"))
                .eventType((String) row.get("event_type"))
                .eventData((String) row.get("event_data"))
                .version((Long) row.get("version"))
                .timestamp((LocalDateTime) row.get("timestamp"))
                .metadata(eventSerializer.deserializeMetadata((String) row.get("metadata")))
                .build();
    }

    private Map<String, Object> createMetadata(DomainEvent event) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("causationId", event.getCausationId());
        metadata.put("correlationId", event.getCorrelationId());
        metadata.put("userId", event.getUserId());
        metadata.put("source", "event-sourcing-service");
        metadata.put("schemaVersion", "1.0");
        return metadata;
    }

    // Supporting classes and methods
    private Mono<AggregateSnapshot> getLatestSnapshot(String aggregateId) {
        return snapshotManager.getLatestSnapshot(aggregateId);
    }

    private Mono<AggregateRoot> createEmptyAggregate(Class<? extends AggregateRoot> aggregateType) {
        try {
            return Mono.just(aggregateType.getDeclaredConstructor().newInstance());
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Failed to create aggregate instance", e));
        }
    }

    private Mono<Long> getEventCount(String aggregateId) {
        return databaseClient.sql("SELECT COUNT(*) FROM event_store WHERE aggregate_id = $1")
                .bind("$1", aggregateId)
                .fetch()
                .one()
                .map(row -> (Long) row.get("count"));
    }

    private Mono<LocalDateTime> getFirstEventTimestamp(String aggregateId) {
        return databaseClient.sql("SELECT MIN(timestamp) FROM event_store WHERE aggregate_id = $1")
                .bind("$1", aggregateId)
                .fetch()
                .one()
                .map(row -> (LocalDateTime) row.get("min"));
    }

    private Mono<LocalDateTime> getLastEventTimestamp(String aggregateId) {
        return databaseClient.sql("SELECT MAX(timestamp) FROM event_store WHERE aggregate_id = $1")
                .bind("$1", aggregateId)
                .fetch()
                .one()
                .map(row -> (LocalDateTime) row.get("max"));
    }

    private Mono<List<String>> getEventTypes(String aggregateId) {
        return databaseClient.sql("SELECT DISTINCT event_type FROM event_store WHERE aggregate_id = $1")
                .bind("$1", aggregateId)
                .fetch()
                .all()
                .map(row -> (String) row.get("event_type"))
                .collectList();
    }

    // Data classes
    public static class EventRecord {
        private String aggregateId;
        private String eventId;
        private String eventType;
        private String eventData;
        private long version;
        private LocalDateTime timestamp;
        private Map<String, Object> metadata;

        // Builder pattern implementation
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private EventRecord record = new EventRecord();

            public Builder aggregateId(String aggregateId) {
                record.aggregateId = aggregateId;
                return this;
            }

            public Builder eventId(String eventId) {
                record.eventId = eventId;
                return this;
            }

            public Builder eventType(String eventType) {
                record.eventType = eventType;
                return this;
            }

            public Builder eventData(String eventData) {
                record.eventData = eventData;
                return this;
            }

            public Builder version(long version) {
                record.version = version;
                return this;
            }

            public Builder timestamp(LocalDateTime timestamp) {
                record.timestamp = timestamp;
                return this;
            }

            public Builder metadata(Map<String, Object> metadata) {
                record.metadata = metadata;
                return this;
            }

            public EventRecord build() {
                return record;
            }
        }

        // Getters
        public String getAggregateId() { return aggregateId; }
        public String getEventId() { return eventId; }
        public String getEventType() { return eventType; }
        public String getEventData() { return eventData; }
        public long getVersion() { return version; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public Map<String, Object> getMetadata() { return metadata; }
    }

    public static class EventStreamStatistics {
        private String aggregateId;
        private long eventCount;
        private LocalDateTime firstEventTimestamp;
        private LocalDateTime lastEventTimestamp;
        private List<String> eventTypes;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private EventStreamStatistics stats = new EventStreamStatistics();

            public Builder aggregateId(String aggregateId) {
                stats.aggregateId = aggregateId;
                return this;
            }

            public Builder eventCount(long eventCount) {
                stats.eventCount = eventCount;
                return this;
            }

            public Builder firstEventTimestamp(LocalDateTime timestamp) {
                stats.firstEventTimestamp = timestamp;
                return this;
            }

            public Builder lastEventTimestamp(LocalDateTime timestamp) {
                stats.lastEventTimestamp = timestamp;
                return this;
            }

            public Builder eventTypes(List<String> eventTypes) {
                stats.eventTypes = eventTypes;
                return this;
            }

            public EventStreamStatistics build() {
                return stats;
            }
        }

        // Getters
        public String getAggregateId() { return aggregateId; }
        public long getEventCount() { return eventCount; }
        public LocalDateTime getFirstEventTimestamp() { return firstEventTimestamp; }
        public LocalDateTime getLastEventTimestamp() { return lastEventTimestamp; }
        public List<String> getEventTypes() { return eventTypes; }
    }

    // Exception classes
    public static class OptimisticLockException extends RuntimeException {
        public OptimisticLockException(String message) {
            super(message);
        }
    }

    public static class ConcurrencyException extends RuntimeException {
        public ConcurrencyException(String message) {
            super(message);
        }
    }
}
```

## Part 2: CQRS Implementation with Advanced Projections

### Command and Query Separation Architecture

```java
package com.distributed.cqrs.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

@Service
public class CommandProcessor {

    private static final Logger logger = LoggerFactory.getLogger(CommandProcessor.class);

    private final CommandValidationService validationService;
    private final AggregateRepository aggregateRepository;
    private final AdvancedEventStore eventStore;
    private final CommandBus commandBus;

    public CommandProcessor(CommandValidationService validationService,
            AggregateRepository aggregateRepository,
            AdvancedEventStore eventStore,
            CommandBus commandBus) {
        this.validationService = validationService;
        this.aggregateRepository = aggregateRepository;
        this.eventStore = eventStore;
        this.commandBus = commandBus;
    }

    @Transactional
    public <T extends Command> Mono<CommandResult> processCommand(T command) {
        logger.info("Processing command: {} with ID: {}", 
                command.getClass().getSimpleName(), command.getCommandId());

        return validateCommand(command)
                .flatMap(validCommand -> loadOrCreateAggregate(validCommand))
                .flatMap(context -> executeCommand(context))
                .flatMap(context -> persistEvents(context))
                .map(context -> createSuccessResult(context))
                .doOnSuccess(result -> 
                    logger.info("Successfully processed command: {} in {}ms", 
                            command.getCommandId(), result.getProcessingTimeMs()))
                .onErrorMap(this::handleCommandError);
    }

    public <T extends Command> CompletableFuture<CommandResult> processCommandAsync(T command) {
        return processCommand(command).toFuture();
    }

    private <T extends Command> Mono<T> validateCommand(T command) {
        return validationService.validate(command)
                .filter(ValidationResult::isValid)
                .map(validationResult -> command)
                .switchIfEmpty(Mono.error(new CommandValidationException(
                        "Command validation failed: " + command.getCommandId())));
    }

    private <T extends Command> Mono<CommandExecutionContext<T>> loadOrCreateAggregate(T command) {
        return aggregateRepository.findById(command.getAggregateId())
                .cast(AggregateRoot.class)
                .switchIfEmpty(createNewAggregate(command.getAggregateType()))
                .map(aggregate -> CommandExecutionContext.<T>builder()
                        .command(command)
                        .aggregate(aggregate)
                        .originalVersion(aggregate.getVersion())
                        .startTime(System.currentTimeMillis())
                        .build());
    }

    private Mono<AggregateRoot> createNewAggregate(Class<? extends AggregateRoot> aggregateType) {
        try {
            AggregateRoot aggregate = aggregateType.getDeclaredConstructor().newInstance();
            return Mono.just(aggregate);
        } catch (Exception e) {
            return Mono.error(new AggregateCreationException(
                    "Failed to create aggregate of type: " + aggregateType.getSimpleName(), e));
        }
    }

    private <T extends Command> Mono<CommandExecutionContext<T>> executeCommand(
            CommandExecutionContext<T> context) {
        
        try {
            // Use reflection or command handler registry to execute command
            CommandHandler<T> handler = commandBus.getHandler(context.getCommand().getClass());
            List<DomainEvent> events = handler.handle(context.getCommand(), context.getAggregate());
            
            // Apply events to aggregate
            events.forEach(event -> context.getAggregate().apply(event));
            
            context.setGeneratedEvents(events);
            return Mono.just(context);
            
        } catch (Exception e) {
            return Mono.error(new CommandExecutionException(
                    "Failed to execute command: " + context.getCommand().getCommandId(), e));
        }
    }

    private <T extends Command> Mono<CommandExecutionContext<T>> persistEvents(
            CommandExecutionContext<T> context) {
        
        if (context.getGeneratedEvents().isEmpty()) {
            return Mono.just(context);
        }

        return eventStore.appendEvents(
                        context.getCommand().getAggregateId(),
                        context.getGeneratedEvents(),
                        context.getOriginalVersion())
                .map(newVersion -> {
                    context.setNewVersion(newVersion);
                    return context;
                });
    }

    private <T extends Command> CommandResult createSuccessResult(CommandExecutionContext<T> context) {
        return CommandResult.builder()
                .commandId(context.getCommand().getCommandId())
                .success(true)
                .newVersion(context.getNewVersion())
                .eventsGenerated(context.getGeneratedEvents().size())
                .processingTimeMs(System.currentTimeMillis() - context.getStartTime())
                .build();
    }

    private RuntimeException handleCommandError(Throwable error) {
        if (error instanceof CommandException) {
            return (CommandException) error;
        }
        logger.error("Unexpected error processing command", error);
        return new CommandProcessingException("Unexpected error processing command", error);
    }

    // Supporting classes
    private static class CommandExecutionContext<T extends Command> {
        private T command;
        private AggregateRoot aggregate;
        private long originalVersion;
        private long newVersion;
        private List<DomainEvent> generatedEvents;
        private long startTime;

        public static <T extends Command> Builder<T> builder() {
            return new Builder<>();
        }

        public static class Builder<T extends Command> {
            private CommandExecutionContext<T> context = new CommandExecutionContext<>();

            public Builder<T> command(T command) {
                context.command = command;
                return this;
            }

            public Builder<T> aggregate(AggregateRoot aggregate) {
                context.aggregate = aggregate;
                return this;
            }

            public Builder<T> originalVersion(long version) {
                context.originalVersion = version;
                return this;
            }

            public Builder<T> startTime(long startTime) {
                context.startTime = startTime;
                return this;
            }

            public CommandExecutionContext<T> build() {
                return context;
            }
        }

        // Getters and setters
        public T getCommand() { return command; }
        public AggregateRoot getAggregate() { return aggregate; }
        public long getOriginalVersion() { return originalVersion; }
        public long getNewVersion() { return newVersion; }
        public void setNewVersion(long newVersion) { this.newVersion = newVersion; }
        public List<DomainEvent> getGeneratedEvents() { return generatedEvents; }
        public void setGeneratedEvents(List<DomainEvent> events) { this.generatedEvents = events; }
        public long getStartTime() { return startTime; }
    }

    // Exception classes
    public static class CommandException extends RuntimeException {
        public CommandException(String message) { super(message); }
        public CommandException(String message, Throwable cause) { super(message, cause); }
    }

    public static class CommandValidationException extends CommandException {
        public CommandValidationException(String message) { super(message); }
    }

    public static class CommandExecutionException extends CommandException {
        public CommandExecutionException(String message, Throwable cause) { super(message, cause); }
    }

    public static class CommandProcessingException extends CommandException {
        public CommandProcessingException(String message, Throwable cause) { super(message, cause); }
    }

    public static class AggregateCreationException extends CommandException {
        public AggregateCreationException(String message, Throwable cause) { super(message, cause); }
    }
}
```

### Advanced Query Processing with Projections

```java
package com.distributed.cqrs.query;

import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdvancedQueryProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AdvancedQueryProcessor.class);

    private final DatabaseClient readModelDatabase;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final ProjectionManager projectionManager;
    private final QueryCache queryCache;
    private final Map<Class<? extends Query>, QueryHandler<?>> queryHandlers = new ConcurrentHashMap<>();

    public AdvancedQueryProcessor(DatabaseClient readModelDatabase,
            ReactiveRedisTemplate<String, Object> redisTemplate,
            ProjectionManager projectionManager) {
        this.readModelDatabase = readModelDatabase;
        this.redisTemplate = redisTemplate;
        this.projectionManager = projectionManager;
        this.queryCache = new QueryCache(redisTemplate);
        registerQueryHandlers();
    }

    public <T extends Query, R> Mono<QueryResult<R>> processQuery(T query) {
        logger.debug("Processing query: {} with ID: {}", 
                query.getClass().getSimpleName(), query.getQueryId());

        return validateQuery(query)
                .flatMap(validQuery -> checkCache(validQuery))
                .switchIfEmpty(executeQuery(query))
                .map(result -> QueryResult.<R>builder()
                        .queryId(query.getQueryId())
                        .data(result)
                        .timestamp(LocalDateTime.now())
                        .cached(false)
                        .build())
                .doOnSuccess(result -> cacheResult(query, result))
                .doOnSuccess(result -> 
                    logger.debug("Successfully processed query: {}", query.getQueryId()));
    }

    public <T extends Query, R> Flux<R> processStreamingQuery(T query) {
        return validateQuery(query)
                .flatMapMany(validQuery -> executeStreamingQuery(validQuery))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @SuppressWarnings("unchecked")
    private <T extends Query, R> Mono<R> executeQuery(T query) {
        QueryHandler<T> handler = (QueryHandler<T>) queryHandlers.get(query.getClass());
        
        if (handler == null) {
            return Mono.error(new QueryHandlerNotFoundException(
                    "No handler found for query type: " + query.getClass().getSimpleName()));
        }

        return handler.handle(query)
                .cast(Object.class)
                .map(result -> (R) result);
    }

    @SuppressWarnings("unchecked")
    private <T extends Query, R> Flux<R> executeStreamingQuery(T query) {
        if (query instanceof StreamingQuery) {
            StreamingQueryHandler<T> handler = (StreamingQueryHandler<T>) queryHandlers.get(query.getClass());
            
            if (handler == null) {
                return Flux.error(new QueryHandlerNotFoundException(
                        "No streaming handler found for query type: " + query.getClass().getSimpleName()));
            }

            return handler.handleStream(query)
                    .cast(Object.class)
                    .map(result -> (R) result);
        }

        return Flux.error(new UnsupportedOperationException(
                "Query does not support streaming: " + query.getClass().getSimpleName()));
    }

    private <T extends Query> Mono<T> validateQuery(T query) {
        // Add query validation logic here
        if (query.getQueryId() == null || query.getQueryId().isEmpty()) {
            return Mono.error(new QueryValidationException("Query ID is required"));
        }
        return Mono.just(query);
    }

    private <T extends Query> Mono<Object> checkCache(T query) {
        if (!query.isCacheable()) {
            return Mono.empty();
        }

        return queryCache.get(query.getCacheKey())
                .doOnNext(cached -> logger.debug("Cache hit for query: {}", query.getQueryId()));
    }

    private <T extends Query, R> void cacheResult(T query, QueryResult<R> result) {
        if (query.isCacheable() && query.getCacheTtl() != null) {
            queryCache.put(query.getCacheKey(), result.getData(), query.getCacheTtl())
                    .subscribe(cached -> 
                        logger.debug("Cached result for query: {}", query.getQueryId()));
        }
    }

    private void registerQueryHandlers() {
        // Account query handlers
        queryHandlers.put(GetAccountQuery.class, new GetAccountQueryHandler(readModelDatabase));
        queryHandlers.put(GetAccountHistoryQuery.class, new GetAccountHistoryQueryHandler(readModelDatabase));
        queryHandlers.put(SearchAccountsQuery.class, new SearchAccountsQueryHandler(readModelDatabase));
        
        // Transaction query handlers
        queryHandlers.put(GetTransactionQuery.class, new GetTransactionQueryHandler(readModelDatabase));
        queryHandlers.put(GetTransactionHistoryQuery.class, new GetTransactionHistoryQueryHandler(readModelDatabase));
        
        // Streaming query handlers
        queryHandlers.put(AccountBalanceStreamQuery.class, new AccountBalanceStreamHandler(projectionManager));
        queryHandlers.put(TransactionStreamQuery.class, new TransactionStreamHandler(projectionManager));
        
        // Analytics query handlers
        queryHandlers.put(AccountAnalyticsQuery.class, new AccountAnalyticsQueryHandler(readModelDatabase));
        queryHandlers.put(TransactionAnalyticsQuery.class, new TransactionAnalyticsQueryHandler(readModelDatabase));
    }

    // Query Handler Implementations
    private static class GetAccountQueryHandler implements QueryHandler<GetAccountQuery> {
        private final DatabaseClient databaseClient;

        public GetAccountQueryHandler(DatabaseClient databaseClient) {
            this.databaseClient = databaseClient;
        }

        @Override
        public Mono<AccountReadModel> handle(GetAccountQuery query) {
            String sql = """
                    SELECT account_id, account_number, account_type, balance, status, 
                           created_at, updated_at, version
                    FROM account_read_model 
                    WHERE account_id = $1
                    """;

            return databaseClient.sql(sql)
                    .bind("$1", query.getAccountId())
                    .fetch()
                    .one()
                    .map(this::mapToAccountReadModel)
                    .switchIfEmpty(Mono.error(new AccountNotFoundException(
                            "Account not found: " + query.getAccountId())));
        }

        private AccountReadModel mapToAccountReadModel(Map<String, Object> row) {
            return AccountReadModel.builder()
                    .accountId((String) row.get("account_id"))
                    .accountNumber((String) row.get("account_number"))
                    .accountType((String) row.get("account_type"))
                    .balance((Double) row.get("balance"))
                    .status((String) row.get("status"))
                    .createdAt((LocalDateTime) row.get("created_at"))
                    .updatedAt((LocalDateTime) row.get("updated_at"))
                    .version((Long) row.get("version"))
                    .build();
        }
    }

    private static class AccountBalanceStreamHandler implements StreamingQueryHandler<AccountBalanceStreamQuery> {
        private final ProjectionManager projectionManager;

        public AccountBalanceStreamHandler(ProjectionManager projectionManager) {
            this.projectionManager = projectionManager;
        }

        @Override
        public Flux<AccountBalanceProjection> handleStream(AccountBalanceStreamQuery query) {
            return projectionManager.getProjectionStream("account-balance", query.getAccountId())
                    .cast(AccountBalanceProjection.class)
                    .filter(projection -> projection.getAccountId().equals(query.getAccountId()));
        }
    }

    private static class SearchAccountsQueryHandler implements QueryHandler<SearchAccountsQuery> {
        private final DatabaseClient databaseClient;

        public SearchAccountsQueryHandler(DatabaseClient databaseClient) {
            this.databaseClient = databaseClient;
        }

        @Override
        public Mono<List<AccountReadModel>> handle(SearchAccountsQuery query) {
            StringBuilder sqlBuilder = new StringBuilder("""
                    SELECT account_id, account_number, account_type, balance, status, 
                           created_at, updated_at, version
                    FROM account_read_model 
                    WHERE 1=1
                    """);

            List<Object> params = new ArrayList<>();
            int paramIndex = 1;

            if (query.getAccountType() != null) {
                sqlBuilder.append(" AND account_type = $").append(paramIndex++);
                params.add(query.getAccountType());
            }

            if (query.getStatus() != null) {
                sqlBuilder.append(" AND status = $").append(paramIndex++);
                params.add(query.getStatus());
            }

            if (query.getMinBalance() != null) {
                sqlBuilder.append(" AND balance >= $").append(paramIndex++);
                params.add(query.getMinBalance());
            }

            if (query.getMaxBalance() != null) {
                sqlBuilder.append(" AND balance <= $").append(paramIndex++);
                params.add(query.getMaxBalance());
            }

            sqlBuilder.append(" ORDER BY created_at DESC");

            if (query.getLimit() != null) {
                sqlBuilder.append(" LIMIT $").append(paramIndex++);
                params.add(query.getLimit());
            }

            if (query.getOffset() != null) {
                sqlBuilder.append(" OFFSET $").append(paramIndex);
                params.add(query.getOffset());
            }

            DatabaseClient.GenericExecuteSpec executeSpec = databaseClient.sql(sqlBuilder.toString());
            
            for (int i = 0; i < params.size(); i++) {
                executeSpec = executeSpec.bind("$" + (i + 1), params.get(i));
            }

            return executeSpec.fetch()
                    .all()
                    .map(this::mapToAccountReadModel)
                    .collectList();
        }

        private AccountReadModel mapToAccountReadModel(Map<String, Object> row) {
            return AccountReadModel.builder()
                    .accountId((String) row.get("account_id"))
                    .accountNumber((String) row.get("account_number"))
                    .accountType((String) row.get("account_type"))
                    .balance((Double) row.get("balance"))
                    .status((String) row.get("status"))
                    .createdAt((LocalDateTime) row.get("created_at"))
                    .updatedAt((LocalDateTime) row.get("updated_at"))
                    .version((Long) row.get("version"))
                    .build();
        }
    }

    // Supporting interfaces and classes
    public interface QueryHandler<T extends Query> {
        Mono<?> handle(T query);
    }

    public interface StreamingQueryHandler<T extends Query> {
        Flux<?> handleStream(T query);
    }

    public static class QueryResult<T> {
        private String queryId;
        private T data;
        private LocalDateTime timestamp;
        private boolean cached;

        public static <T> Builder<T> builder() {
            return new Builder<>();
        }

        public static class Builder<T> {
            private QueryResult<T> result = new QueryResult<>();

            public Builder<T> queryId(String queryId) {
                result.queryId = queryId;
                return this;
            }

            public Builder<T> data(T data) {
                result.data = data;
                return this;
            }

            public Builder<T> timestamp(LocalDateTime timestamp) {
                result.timestamp = timestamp;
                return this;
            }

            public Builder<T> cached(boolean cached) {
                result.cached = cached;
                return this;
            }

            public QueryResult<T> build() {
                return result;
            }
        }

        // Getters
        public String getQueryId() { return queryId; }
        public T getData() { return data; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public boolean isCached() { return cached; }
    }

    // Exception classes
    public static class QueryHandlerNotFoundException extends RuntimeException {
        public QueryHandlerNotFoundException(String message) { super(message); }
    }

    public static class QueryValidationException extends RuntimeException {
        public QueryValidationException(String message) { super(message); }
    }

    public static class AccountNotFoundException extends RuntimeException {
        public AccountNotFoundException(String message) { super(message); }
    }
}
```

## Part 3: Saga Orchestration Patterns

### Distributed Saga Implementation

```java
package com.distributed.saga.orchestration;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SagaOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(SagaOrchestrator.class);

    private final SagaStateRepository sagaStateRepository;
    private final CommandGateway commandGateway;
    private final EventPublisher eventPublisher;
    private final SagaCompensationService compensationService;
    private final Map<String, SagaDefinition> sagaDefinitions = new ConcurrentHashMap<>();
    private final Map<String, SagaExecution> activeSagas = new ConcurrentHashMap<>();

    public SagaOrchestrator(SagaStateRepository sagaStateRepository,
            CommandGateway commandGateway,
            EventPublisher eventPublisher,
            SagaCompensationService compensationService) {
        this.sagaStateRepository = sagaStateRepository;
        this.commandGateway = commandGateway;
        this.eventPublisher = eventPublisher;
        this.compensationService = compensationService;
        initializeSagaDefinitions();
    }

    public Mono<SagaExecutionResult> startSaga(String sagaType, Map<String, Object> sagaData) {
        String sagaId = UUID.randomUUID().toString();
        
        logger.info("Starting saga: {} with ID: {}", sagaType, sagaId);

        return createSagaExecution(sagaId, sagaType, sagaData)
                .flatMap(this::persistSagaState)
                .flatMap(this::executeNextStep)
                .map(execution -> SagaExecutionResult.builder()
                        .sagaId(sagaId)
                        .sagaType(sagaType)
                        .status(execution.getStatus())
                        .currentStep(execution.getCurrentStep())
                        .message("Saga started successfully")
                        .build())
                .doOnSuccess(result -> activeSagas.put(sagaId, 
                        activeSagas.get(sagaId).withStatus(SagaStatus.RUNNING)))
                .onErrorMap(error -> new SagaExecutionException(
                        "Failed to start saga: " + sagaId, error));
    }

    public Mono<SagaExecutionResult> handleSagaEvent(String sagaId, SagaEvent event) {
        logger.debug("Handling saga event: {} for saga: {}", event.getEventType(), sagaId);

        return getSagaExecution(sagaId)
                .flatMap(execution -> processEvent(execution, event))
                .flatMap(this::executeNextStep)
                .flatMap(this::persistSagaState)
                .map(execution -> SagaExecutionResult.builder()
                        .sagaId(sagaId)
                        .sagaType(execution.getSagaType())
                        .status(execution.getStatus())
                        .currentStep(execution.getCurrentStep())
                        .message("Event processed successfully")
                        .build())
                .doOnSuccess(result -> {
                    if (result.getStatus() == SagaStatus.COMPLETED || 
                        result.getStatus() == SagaStatus.COMPENSATED) {
                        activeSagas.remove(sagaId);
                    }
                });
    }

    public Mono<SagaExecutionResult> compensateSaga(String sagaId, String reason) {
        logger.warn("Compensating saga: {} due to: {}", sagaId, reason);

        return getSagaExecution(sagaId)
                .flatMap(execution -> startCompensation(execution, reason))
                .flatMap(this::executeCompensationSteps)
                .flatMap(this::persistSagaState)
                .map(execution -> SagaExecutionResult.builder()
                        .sagaId(sagaId)
                        .sagaType(execution.getSagaType())
                        .status(execution.getStatus())
                        .currentStep(execution.getCurrentStep())
                        .message("Saga compensation completed")
                        .build());
    }

    public Flux<SagaExecution> getActiveSagas() {
        return Flux.fromIterable(activeSagas.values());
    }

    public Mono<SagaExecution> getSagaStatus(String sagaId) {
        return Mono.fromCallable(() -> activeSagas.get(sagaId))
                .switchIfEmpty(sagaStateRepository.findById(sagaId));
    }

    private Mono<SagaExecution> createSagaExecution(String sagaId, String sagaType, 
            Map<String, Object> sagaData) {
        
        SagaDefinition definition = sagaDefinitions.get(sagaType);
        if (definition == null) {
            return Mono.error(new SagaDefinitionNotFoundException(
                    "Saga definition not found: " + sagaType));
        }

        SagaExecution execution = SagaExecution.builder()
                .sagaId(sagaId)
                .sagaType(sagaType)
                .status(SagaStatus.STARTED)
                .currentStep(0)
                .sagaData(new HashMap<>(sagaData))
                .startTime(LocalDateTime.now())
                .definition(definition)
                .build();

        activeSagas.put(sagaId, execution);
        return Mono.just(execution);
    }

    private Mono<SagaExecution> executeNextStep(SagaExecution execution) {
        if (execution.getStatus() != SagaStatus.RUNNING && 
            execution.getStatus() != SagaStatus.STARTED) {
            return Mono.just(execution);
        }

        List<SagaStep> steps = execution.getDefinition().getSteps();
        
        if (execution.getCurrentStep() >= steps.size()) {
            return Mono.just(execution.withStatus(SagaStatus.COMPLETED));
        }

        SagaStep currentStep = steps.get(execution.getCurrentStep());
        
        return executeStep(execution, currentStep)
                .map(updatedExecution -> updatedExecution.withStatus(SagaStatus.RUNNING))
                .onErrorResume(error -> handleStepError(execution, currentStep, error));
    }

    private Mono<SagaExecution> executeStep(SagaExecution execution, SagaStep step) {
        logger.debug("Executing saga step: {} for saga: {}", step.getStepName(), execution.getSagaId());

        return createStepCommand(execution, step)
                .flatMap(command -> commandGateway.send(command))
                .map(result -> {
                    execution.getSagaData().putAll(result.getData());
                    execution.getExecutedSteps().add(ExecutedStep.builder()
                            .stepName(step.getStepName())
                            .command(step.getCommand())
                            .executedAt(LocalDateTime.now())
                            .status(ExecutionStatus.COMPLETED)
                            .result(result)
                            .build());
                    return execution.withCurrentStep(execution.getCurrentStep() + 1);
                })
                .timeout(Duration.ofSeconds(step.getTimeoutSeconds()))
                .onErrorMap(error -> new SagaStepExecutionException(
                        "Failed to execute step: " + step.getStepName(), error));
    }

    private Mono<SagaExecution> handleStepError(SagaExecution execution, SagaStep step, Throwable error) {
        logger.error("Step execution failed for saga: {} step: {}", 
                execution.getSagaId(), step.getStepName(), error);

        execution.getExecutedSteps().add(ExecutedStep.builder()
                .stepName(step.getStepName())
                .command(step.getCommand())
                .executedAt(LocalDateTime.now())
                .status(ExecutionStatus.FAILED)
                .error(error.getMessage())
                .build());

        if (step.isCompensatable()) {
            return startCompensation(execution, "Step execution failed: " + error.getMessage());
        } else {
            return Mono.just(execution.withStatus(SagaStatus.FAILED));
        }
    }

    private Mono<SagaExecution> startCompensation(SagaExecution execution, String reason) {
        logger.warn("Starting compensation for saga: {} reason: {}", execution.getSagaId(), reason);

        return Mono.just(execution
                .withStatus(SagaStatus.COMPENSATING)
                .withCompensationReason(reason)
                .withCompensationStartTime(LocalDateTime.now()));
    }

    private Mono<SagaExecution> executeCompensationSteps(SagaExecution execution) {
        List<ExecutedStep> stepsToCompensate = execution.getExecutedSteps().stream()
                .filter(step -> step.getStatus() == ExecutionStatus.COMPLETED)
                .sorted((a, b) -> b.getExecutedAt().compareTo(a.getExecutedAt())) // Reverse order
                .toList();

        return Flux.fromIterable(stepsToCompensate)
                .flatMap(step -> compensateStep(execution, step), 1) // Sequential compensation
                .then(Mono.just(execution.withStatus(SagaStatus.COMPENSATED)));
    }

    private Mono<Void> compensateStep(SagaExecution execution, ExecutedStep executedStep) {
        return compensationService.compensate(execution.getSagaId(), executedStep)
                .doOnSuccess(result -> 
                    logger.debug("Compensated step: {} for saga: {}", 
                            executedStep.getStepName(), execution.getSagaId()))
                .onErrorResume(error -> {
                    logger.error("Failed to compensate step: {} for saga: {}", 
                            executedStep.getStepName(), execution.getSagaId(), error);
                    return Mono.empty(); // Continue with other compensations
                });
    }

    private Mono<SagaExecution> processEvent(SagaExecution execution, SagaEvent event) {
        // Update saga data based on event
        execution.getSagaData().putAll(event.getEventData());
        
        // Update execution status based on event type
        switch (event.getEventType()) {
            case "STEP_COMPLETED":
                return Mono.just(execution);
            case "STEP_FAILED":
                return Mono.just(execution.withStatus(SagaStatus.COMPENSATING));
            default:
                return Mono.just(execution);
        }
    }

    private Mono<Command> createStepCommand(SagaExecution execution, SagaStep step) {
        // Create command based on step definition and saga data
        Map<String, Object> commandData = new HashMap<>(execution.getSagaData());
        commandData.put("sagaId", execution.getSagaId());
        commandData.put("stepName", step.getStepName());

        return Mono.just(Command.builder()
                .commandId(UUID.randomUUID().toString())
                .commandType(step.getCommand())
                .targetService(step.getTargetService())
                .commandData(commandData)
                .timeout(Duration.ofSeconds(step.getTimeoutSeconds()))
                .build());
    }

    private Mono<SagaExecution> getSagaExecution(String sagaId) {
        SagaExecution activeExecution = activeSagas.get(sagaId);
        if (activeExecution != null) {
            return Mono.just(activeExecution);
        }
        
        return sagaStateRepository.findById(sagaId)
                .switchIfEmpty(Mono.error(new SagaNotFoundException("Saga not found: " + sagaId)));
    }

    private Mono<SagaExecution> persistSagaState(SagaExecution execution) {
        return sagaStateRepository.save(execution)
                .doOnSuccess(saved -> activeSagas.put(execution.getSagaId(), saved));
    }

    private void initializeSagaDefinitions() {
        // Order Processing Saga
        sagaDefinitions.put("ORDER_PROCESSING", SagaDefinition.builder()
                .sagaType("ORDER_PROCESSING")
                .description("Process customer order with payment and inventory")
                .steps(Arrays.asList(
                        SagaStep.builder()
                                .stepName("RESERVE_INVENTORY")
                                .command("ReserveInventoryCommand")
                                .targetService("inventory-service")
                                .timeoutSeconds(30)
                                .compensatable(true)
                                .compensationCommand("ReleaseInventoryCommand")
                                .build(),
                        SagaStep.builder()
                                .stepName("PROCESS_PAYMENT")
                                .command("ProcessPaymentCommand")
                                .targetService("payment-service")
                                .timeoutSeconds(60)
                                .compensatable(true)
                                .compensationCommand("RefundPaymentCommand")
                                .build(),
                        SagaStep.builder()
                                .stepName("SHIP_ORDER")
                                .command("ShipOrderCommand")
                                .targetService("shipping-service")
                                .timeoutSeconds(120)
                                .compensatable(false)
                                .build()
                ))
                .build());

        // Account Transfer Saga
        sagaDefinitions.put("ACCOUNT_TRANSFER", SagaDefinition.builder()
                .sagaType("ACCOUNT_TRANSFER")
                .description("Transfer money between accounts")
                .steps(Arrays.asList(
                        SagaStep.builder()
                                .stepName("DEBIT_SOURCE_ACCOUNT")
                                .command("DebitAccountCommand")
                                .targetService("account-service")
                                .timeoutSeconds(30)
                                .compensatable(true)
                                .compensationCommand("CreditAccountCommand")
                                .build(),
                        SagaStep.builder()
                                .stepName("CREDIT_TARGET_ACCOUNT")
                                .command("CreditAccountCommand")
                                .targetService("account-service")
                                .timeoutSeconds(30)
                                .compensatable(true)
                                .compensationCommand("DebitAccountCommand")
                                .build(),
                        SagaStep.builder()
                                .stepName("RECORD_TRANSFER")
                                .command("RecordTransferCommand")
                                .targetService("transaction-service")
                                .timeoutSeconds(15)
                                .compensatable(false)
                                .build()
                ))
                .build());
    }

    // Supporting classes and enums
    public enum SagaStatus {
        STARTED, RUNNING, COMPLETED, FAILED, COMPENSATING, COMPENSATED
    }

    public enum ExecutionStatus {
        PENDING, COMPLETED, FAILED, COMPENSATED
    }

    // Exception classes
    public static class SagaExecutionException extends RuntimeException {
        public SagaExecutionException(String message, Throwable cause) { super(message, cause); }
    }

    public static class SagaDefinitionNotFoundException extends RuntimeException {
        public SagaDefinitionNotFoundException(String message) { super(message); }
    }

    public static class SagaNotFoundException extends RuntimeException {
        public SagaNotFoundException(String message) { super(message); }
    }

    public static class SagaStepExecutionException extends RuntimeException {
        public SagaStepExecutionException(String message, Throwable cause) { super(message, cause); }
    }
}
```

## Practical Exercises

### Exercise 1: Event Store Optimization
Implement advanced event store features including:
- Event stream sharding for high throughput
- Custom serialization formats (Avro, Protocol Buffers)
- Event versioning and schema evolution
- Performance benchmarking and optimization

### Exercise 2: CQRS with Multiple Read Models
Build a comprehensive CQRS system with:
- Multiple specialized read models for different query patterns
- Real-time projection updates with event streaming
- Materialized views for complex analytical queries
- Eventual consistency handling and conflict resolution

### Exercise 3: Complex Saga Orchestration
Design and implement complex saga patterns:
- Nested sagas with sub-transactions
- Parallel execution branches with synchronization points
- Advanced compensation strategies with partial rollbacks
- Saga monitoring and visualization dashboard

### Exercise 4: Event Sourcing Performance Testing
Create comprehensive performance tests for:
- High-volume event ingestion (100k+ events/second)
- Concurrent aggregate rebuilding
- Snapshot creation and optimization
- Read model projection latency measurement

### Exercise 5: Production-Ready Event Store
Build a production-ready event store with:
- Multi-region replication and disaster recovery
- Backup and restore capabilities
- Monitoring and alerting integration
- Security and access control implementation

## Performance Considerations

### Event Store Optimization
- **Sharding Strategy**: Distribute events across multiple partitions for scalability
- **Snapshot Management**: Optimize snapshot frequency and storage
- **Read Model Updates**: Use efficient projection mechanisms with minimal latency
- **Caching Strategy**: Implement multi-level caching for frequently accessed aggregates

### CQRS Performance
- **Query Optimization**: Design read models for specific query patterns
- **Projection Efficiency**: Use incremental updates and batch processing
- **Cache Management**: Implement intelligent caching with invalidation strategies
- **Connection Pooling**: Optimize database connections for read and write operations

## Advanced Patterns Best Practices
- Use event sourcing for domains requiring full audit trails and temporal queries
- Implement CQRS when read and write patterns differ significantly
- Design sagas for complex business transactions spanning multiple services
- Ensure proper error handling and compensation in distributed transactions
- Monitor and measure system performance with comprehensive metrics
- Implement proper security and access control for sensitive operations

## Summary

Day 113 covered advanced microservices patterns essential for building sophisticated distributed systems:

1. **Event Sourcing**: Complete event store implementation with advanced features like snapshots, caching, and performance optimization
2. **CQRS Architecture**: Comprehensive command and query separation with advanced projections and read model management
3. **Saga Orchestration**: Distributed transaction coordination with compensation patterns and error handling
4. **Performance Optimization**: Advanced patterns for high-throughput, low-latency distributed systems
5. **Production Readiness**: Monitoring, error handling, and operational considerations for enterprise deployment

These patterns provide the foundation for building resilient, scalable, and maintainable microservices architectures that can handle complex business requirements while maintaining consistency and reliability across distributed systems.