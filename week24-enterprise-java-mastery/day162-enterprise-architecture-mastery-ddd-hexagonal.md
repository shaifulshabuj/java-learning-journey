# Day 162: Enterprise Architecture Mastery - Domain-Driven Design & Hexagonal Architecture

## Learning Objectives
By the end of this lesson, you will:
- Master advanced Domain-Driven Design (DDD) implementation patterns in Java
- Implement Hexagonal (Ports & Adapters) architecture for enterprise applications
- Apply strategic design patterns and bounded contexts
- Use event storming and domain modeling techniques
- Build maintainable, testable enterprise architectures

## 1. Advanced Domain-Driven Design Implementation

### 1.1 Strategic Design and Bounded Contexts

```java
// Strategic Domain Model - E-commerce Platform
package com.enterprise.ecommerce.domain;

/**
 * Strategic Domain Map for E-commerce Platform
 * 
 * Bounded Contexts:
 * - Customer Management
 * - Order Management  
 * - Inventory Management
 * - Payment Processing
 * - Shipping & Fulfillment
 */

// Shared Kernel - Common domain concepts
public class SharedKernel {
    
    @ValueObject
    public record CustomerId(UUID value) {
        public CustomerId {
            Objects.requireNonNull(value, "Customer ID cannot be null");
        }
        
        public static CustomerId generate() {
            return new CustomerId(UUID.randomUUID());
        }
    }
    
    @ValueObject
    public record Money(BigDecimal amount, Currency currency) {
        public Money {
            Objects.requireNonNull(amount, "Amount cannot be null");
            Objects.requireNonNull(currency, "Currency cannot be null");
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Amount cannot be negative");
            }
        }
        
        public Money add(Money other) {
            if (!this.currency.equals(other.currency)) {
                throw new IllegalArgumentException("Cannot add different currencies");
            }
            return new Money(this.amount.add(other.amount), this.currency);
        }
        
        public Money multiply(BigDecimal multiplier) {
            return new Money(this.amount.multiply(multiplier), this.currency);
        }
    }
    
    @ValueObject
    public record ProductId(UUID value) {
        public ProductId {
            Objects.requireNonNull(value, "Product ID cannot be null");
        }
        
        public static ProductId generate() {
            return new ProductId(UUID.randomUUID());
        }
    }
}

// Customer Management Bounded Context
package com.enterprise.ecommerce.customer.domain;

@Entity
@AggregateRoot
public class Customer {
    private final CustomerId customerId;
    private CustomerProfile profile;
    private Set<Address> addresses;
    private CustomerStatus status;
    private List<DomainEvent> domainEvents;
    
    public Customer(CustomerId customerId, CustomerProfile profile) {
        this.customerId = Objects.requireNonNull(customerId);
        this.profile = Objects.requireNonNull(profile);
        this.addresses = new HashSet<>();
        this.status = CustomerStatus.ACTIVE;
        this.domainEvents = new ArrayList<>();
        
        // Domain event for customer creation
        addDomainEvent(new CustomerCreatedEvent(customerId, profile.email()));
    }
    
    public void updateProfile(CustomerProfile newProfile) {
        if (!this.profile.equals(newProfile)) {
            CustomerProfile oldProfile = this.profile;
            this.profile = newProfile;
            addDomainEvent(new CustomerProfileUpdatedEvent(customerId, oldProfile, newProfile));
        }
    }
    
    public void addAddress(Address address) {
        if (addresses.size() >= 5) {
            throw new CustomerBusinessException("Customer cannot have more than 5 addresses");
        }
        addresses.add(address);
        addDomainEvent(new CustomerAddressAddedEvent(customerId, address));
    }
    
    public void deactivate(String reason) {
        if (status == CustomerStatus.INACTIVE) {
            throw new CustomerBusinessException("Customer is already inactive");
        }
        this.status = CustomerStatus.INACTIVE;
        addDomainEvent(new CustomerDeactivatedEvent(customerId, reason));
    }
    
    private void addDomainEvent(DomainEvent event) {
        domainEvents.add(event);
    }
    
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }
    
    public void clearDomainEvents() {
        domainEvents.clear();
    }
    
    // Getters
    public CustomerId getCustomerId() { return customerId; }
    public CustomerProfile getProfile() { return profile; }
    public Set<Address> getAddresses() { return Collections.unmodifiableSet(addresses); }
    public CustomerStatus getStatus() { return status; }
}

@ValueObject
public record CustomerProfile(
    String firstName,
    String lastName,
    Email email,
    PhoneNumber phoneNumber,
    LocalDate dateOfBirth
) {
    public CustomerProfile {
        Objects.requireNonNull(firstName, "First name cannot be null");
        Objects.requireNonNull(lastName, "Last name cannot be null");
        Objects.requireNonNull(email, "Email cannot be null");
        
        if (firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name cannot be empty");
        }
        if (lastName.trim().isEmpty()) {
            throw new IllegalArgumentException("Last name cannot be empty");
        }
    }
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
}

@ValueObject
public record Email(String value) {
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
    
    public Email {
        Objects.requireNonNull(value, "Email cannot be null");
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }
    }
    
    public String getDomain() {
        return value.substring(value.indexOf('@') + 1);
    }
}

@ValueObject
public record Address(
    String street,
    String city,
    String state,
    String postalCode,
    String country,
    AddressType type
) {
    public Address {
        Objects.requireNonNull(street, "Street cannot be null");
        Objects.requireNonNull(city, "City cannot be null");
        Objects.requireNonNull(state, "State cannot be null");
        Objects.requireNonNull(postalCode, "Postal code cannot be null");
        Objects.requireNonNull(country, "Country cannot be null");
        Objects.requireNonNull(type, "Address type cannot be null");
    }
}

public enum AddressType {
    HOME, WORK, BILLING, SHIPPING
}

public enum CustomerStatus {
    ACTIVE, INACTIVE, SUSPENDED
}

// Domain Events
@DomainEvent
public record CustomerCreatedEvent(
    CustomerId customerId,
    Email email,
    Instant occurredOn
) implements DomainEvent {
    public CustomerCreatedEvent(CustomerId customerId, Email email) {
        this(customerId, email, Instant.now());
    }
}

@DomainEvent
public record CustomerProfileUpdatedEvent(
    CustomerId customerId,
    CustomerProfile oldProfile,
    CustomerProfile newProfile,
    Instant occurredOn
) implements DomainEvent {
    public CustomerProfileUpdatedEvent(CustomerId customerId, 
                                     CustomerProfile oldProfile,
                                     CustomerProfile newProfile) {
        this(customerId, oldProfile, newProfile, Instant.now());
    }
}
```

### 1.2 Domain Services and Repository Patterns

```java
// Domain Service for complex business logic
@DomainService
public class CustomerDuplicationCheckService {
    private final CustomerRepository customerRepository;
    
    public CustomerDuplicationCheckService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }
    
    public boolean isDuplicateCustomer(Email email, PhoneNumber phoneNumber) {
        return customerRepository.existsByEmail(email) || 
               customerRepository.existsByPhoneNumber(phoneNumber);
    }
    
    public Optional<Customer> findPotentialDuplicate(CustomerProfile profile) {
        // Complex business logic to identify potential duplicates
        var byEmail = customerRepository.findByEmail(profile.email());
        if (byEmail.isPresent()) {
            return byEmail;
        }
        
        var byPhone = customerRepository.findByPhoneNumber(profile.phoneNumber());
        if (byPhone.isPresent()) {
            return byPhone;
        }
        
        // Fuzzy matching on name and other attributes
        return customerRepository.findSimilarCustomer(
            profile.firstName(), 
            profile.lastName(), 
            profile.dateOfBirth()
        );
    }
}

// Repository Interface (Domain Layer)
public interface CustomerRepository {
    void save(Customer customer);
    Optional<Customer> findById(CustomerId customerId);
    Optional<Customer> findByEmail(Email email);
    Optional<Customer> findByPhoneNumber(PhoneNumber phoneNumber);
    List<Customer> findByStatus(CustomerStatus status);
    boolean existsByEmail(Email email);
    boolean existsByPhoneNumber(PhoneNumber phoneNumber);
    Optional<Customer> findSimilarCustomer(String firstName, String lastName, LocalDate dateOfBirth);
    void delete(CustomerId customerId);
    
    // Specification pattern for complex queries
    List<Customer> findBySpecification(CustomerSpecification specification);
}

// Specification Pattern for Complex Queries
public interface CustomerSpecification {
    boolean isSatisfiedBy(Customer customer);
    
    default CustomerSpecification and(CustomerSpecification other) {
        return new AndSpecification(this, other);
    }
    
    default CustomerSpecification or(CustomerSpecification other) {
        return new OrSpecification(this, other);
    }
    
    default CustomerSpecification not() {
        return new NotSpecification(this);
    }
}

@Component
public class CustomerSpecifications {
    
    public static CustomerSpecification hasStatus(CustomerStatus status) {
        return customer -> customer.getStatus() == status;
    }
    
    public static CustomerSpecification hasEmail(Email email) {
        return customer -> customer.getProfile().email().equals(email);
    }
    
    public static CustomerSpecification registeredAfter(LocalDate date) {
        return customer -> customer.getRegistrationDate().isAfter(date);
    }
    
    public static CustomerSpecification hasAddressInCountry(String country) {
        return customer -> customer.getAddresses().stream()
            .anyMatch(address -> address.country().equalsIgnoreCase(country));
    }
}
```

## 2. Hexagonal Architecture Implementation

### 2.1 Ports and Adapters Structure

```java
// Application Core - Use Cases (Application Layer)
package com.enterprise.ecommerce.customer.application;

@UseCase
@Transactional
public class CreateCustomerUseCase {
    private final CustomerRepository customerRepository;
    private final CustomerDuplicationCheckService duplicationCheckService;
    private final DomainEventPublisher eventPublisher;
    private final CustomerValidator customerValidator;
    
    public CreateCustomerUseCase(CustomerRepository customerRepository,
                               CustomerDuplicationCheckService duplicationCheckService,
                               DomainEventPublisher eventPublisher,
                               CustomerValidator customerValidator) {
        this.customerRepository = customerRepository;
        this.duplicationCheckService = duplicationCheckService;
        this.eventPublisher = eventPublisher;
        this.customerValidator = customerValidator;
    }
    
    public CreateCustomerResponse execute(CreateCustomerCommand command) {
        // Validate command
        var validationResult = customerValidator.validate(command);
        if (!validationResult.isValid()) {
            throw new CustomerValidationException(validationResult.getErrors());
        }
        
        // Check for duplicates
        var profile = mapToCustomerProfile(command);
        if (duplicationCheckService.isDuplicateCustomer(profile.email(), profile.phoneNumber())) {
            throw new DuplicateCustomerException("Customer with email or phone already exists");
        }
        
        // Create customer
        var customerId = CustomerId.generate();
        var customer = new Customer(customerId, profile);
        
        // Add initial address if provided
        if (command.address() != null) {
            customer.addAddress(mapToAddress(command.address()));
        }
        
        // Save customer
        customerRepository.save(customer);
        
        // Publish domain events
        customer.getDomainEvents().forEach(eventPublisher::publish);
        customer.clearDomainEvents();
        
        return new CreateCustomerResponse(customerId, customer.getProfile());
    }
    
    private CustomerProfile mapToCustomerProfile(CreateCustomerCommand command) {
        return new CustomerProfile(
            command.firstName(),
            command.lastName(),
            new Email(command.email()),
            new PhoneNumber(command.phoneNumber()),
            command.dateOfBirth()
        );
    }
    
    private Address mapToAddress(CreateCustomerCommand.AddressDto addressDto) {
        return new Address(
            addressDto.street(),
            addressDto.city(),
            addressDto.state(),
            addressDto.postalCode(),
            addressDto.country(),
            AddressType.valueOf(addressDto.type())
        );
    }
}

// Command and Response Objects
public record CreateCustomerCommand(
    String firstName,
    String lastName,
    String email,
    String phoneNumber,
    LocalDate dateOfBirth,
    AddressDto address
) {
    public record AddressDto(
        String street,
        String city,
        String state,
        String postalCode,
        String country,
        String type
    ) {}
}

public record CreateCustomerResponse(
    CustomerId customerId,
    CustomerProfile profile
) {}

// Port Interfaces (Application Layer)
public interface DomainEventPublisher {
    void publish(DomainEvent event);
    void publishAll(List<DomainEvent> events);
}

public interface CustomerValidator {
    ValidationResult validate(CreateCustomerCommand command);
    ValidationResult validate(UpdateCustomerCommand command);
}

public record ValidationResult(
    boolean valid,
    List<String> errors
) {
    public static ValidationResult success() {
        return new ValidationResult(true, Collections.emptyList());
    }
    
    public static ValidationResult failure(List<String> errors) {
        return new ValidationResult(false, errors);
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }
}
```

### 2.2 Infrastructure Adapters

```java
// JPA Repository Adapter (Infrastructure Layer)
package com.enterprise.ecommerce.customer.infrastructure;

@Repository
@Component
public class JpaCustomerRepositoryAdapter implements CustomerRepository {
    private final JpaCustomerRepository jpaRepository;
    private final CustomerEntityMapper mapper;
    
    public JpaCustomerRepositoryAdapter(JpaCustomerRepository jpaRepository,
                                       CustomerEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    public void save(Customer customer) {
        var entity = mapper.toEntity(customer);
        jpaRepository.save(entity);
    }
    
    @Override
    public Optional<Customer> findById(CustomerId customerId) {
        return jpaRepository.findById(customerId.value())
            .map(mapper::toDomain);
    }
    
    @Override
    public Optional<Customer> findByEmail(Email email) {
        return jpaRepository.findByEmail(email.value())
            .map(mapper::toDomain);
    }
    
    @Override
    public List<Customer> findBySpecification(CustomerSpecification specification) {
        // Convert specification to JPA criteria or use QueryDSL
        return jpaRepository.findAll().stream()
            .map(mapper::toDomain)
            .filter(specification::isSatisfiedBy)
            .collect(Collectors.toList());
    }
    
    @Override
    public Optional<Customer> findSimilarCustomer(String firstName, String lastName, LocalDate dateOfBirth) {
        return jpaRepository.findSimilarCustomer(firstName, lastName, dateOfBirth)
            .map(mapper::toDomain);
    }
    
    // Other methods implemented similarly...
}

// JPA Entity
@Entity
@Table(name = "customers")
public class CustomerEntity {
    @Id
    private UUID id;
    
    @Column(name = "first_name", nullable = false)
    private String firstName;
    
    @Column(name = "last_name", nullable = false)
    private String lastName;
    
    @Column(name = "email", nullable = false, unique = true)
    private String email;
    
    @Column(name = "phone_number")
    private String phoneNumber;
    
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CustomerStatus status;
    
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<AddressEntity> addresses = new HashSet<>();
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    // Constructors, getters, setters...
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

@Entity
@Table(name = "addresses")
public class AddressEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;
    
    @Column(name = "street", nullable = false)
    private String street;
    
    @Column(name = "city", nullable = false)
    private String city;
    
    @Column(name = "state", nullable = false)
    private String state;
    
    @Column(name = "postal_code", nullable = false)
    private String postalCode;
    
    @Column(name = "country", nullable = false)
    private String country;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private AddressType type;
    
    // Constructors, getters, setters...
}

// Domain-Entity Mapper
@Component
public class CustomerEntityMapper {
    
    public CustomerEntity toEntity(Customer customer) {
        var entity = new CustomerEntity();
        entity.setId(customer.getCustomerId().value());
        entity.setFirstName(customer.getProfile().firstName());
        entity.setLastName(customer.getProfile().lastName());
        entity.setEmail(customer.getProfile().email().value());
        entity.setPhoneNumber(customer.getProfile().phoneNumber().value());
        entity.setDateOfBirth(customer.getProfile().dateOfBirth());
        entity.setStatus(customer.getStatus());
        
        var addressEntities = customer.getAddresses().stream()
            .map(address -> toAddressEntity(address, entity))
            .collect(Collectors.toSet());
        entity.setAddresses(addressEntities);
        
        return entity;
    }
    
    public Customer toDomain(CustomerEntity entity) {
        var profile = new CustomerProfile(
            entity.getFirstName(),
            entity.getLastName(),
            new Email(entity.getEmail()),
            new PhoneNumber(entity.getPhoneNumber()),
            entity.getDateOfBirth()
        );
        
        var customer = new Customer(new CustomerId(entity.getId()), profile);
        
        entity.getAddresses().forEach(addressEntity -> {
            var address = toDomainAddress(addressEntity);
            customer.addAddress(address);
        });
        
        return customer;
    }
    
    private AddressEntity toAddressEntity(Address address, CustomerEntity customer) {
        var entity = new AddressEntity();
        entity.setCustomer(customer);
        entity.setStreet(address.street());
        entity.setCity(address.city());
        entity.setState(address.state());
        entity.setPostalCode(address.postalCode());
        entity.setCountry(address.country());
        entity.setType(address.type());
        return entity;
    }
    
    private Address toDomainAddress(AddressEntity entity) {
        return new Address(
            entity.getStreet(),
            entity.getCity(),
            entity.getState(),
            entity.getPostalCode(),
            entity.getCountry(),
            entity.getType()
        );
    }
}
```

### 2.3 Web Adapter (REST API)

```java
// REST Controller (Web Adapter)
package com.enterprise.ecommerce.customer.web;

@RestController
@RequestMapping("/api/v1/customers")
@Validated
public class CustomerController {
    private final CreateCustomerUseCase createCustomerUseCase;
    private final GetCustomerUseCase getCustomerUseCase;
    private final UpdateCustomerUseCase updateCustomerUseCase;
    private final CustomerDtoMapper mapper;
    
    public CustomerController(CreateCustomerUseCase createCustomerUseCase,
                             GetCustomerUseCase getCustomerUseCase,
                             UpdateCustomerUseCase updateCustomerUseCase,
                             CustomerDtoMapper mapper) {
        this.createCustomerUseCase = createCustomerUseCase;
        this.getCustomerUseCase = getCustomerUseCase;
        this.updateCustomerUseCase = updateCustomerUseCase;
        this.mapper = mapper;
    }
    
    @PostMapping
    public ResponseEntity<CustomerResponseDto> createCustomer(
            @Valid @RequestBody CreateCustomerRequestDto request) {
        
        var command = mapper.toCommand(request);
        var response = createCustomerUseCase.execute(command);
        var dto = mapper.toDto(response);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .location(URI.create("/api/v1/customers/" + response.customerId().value()))
            .body(dto);
    }
    
    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponseDto> getCustomer(
            @PathVariable UUID customerId) {
        
        var query = new GetCustomerQuery(new CustomerId(customerId));
        var response = getCustomerUseCase.execute(query);
        var dto = mapper.toDto(response);
        
        return ResponseEntity.ok(dto);
    }
    
    @PutMapping("/{customerId}")
    public ResponseEntity<CustomerResponseDto> updateCustomer(
            @PathVariable UUID customerId,
            @Valid @RequestBody UpdateCustomerRequestDto request) {
        
        var command = mapper.toCommand(customerId, request);
        var response = updateCustomerUseCase.execute(command);
        var dto = mapper.toDto(response);
        
        return ResponseEntity.ok(dto);
    }
    
    @GetMapping
    public ResponseEntity<PagedResponseDto<CustomerSummaryDto>> getCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) CustomerStatus status) {
        
        var query = GetCustomersQuery.builder()
            .page(page)
            .size(size)
            .email(email != null ? new Email(email) : null)
            .status(status)
            .build();
            
        var response = getCustomersUseCase.execute(query);
        var dto = mapper.toPagedDto(response);
        
        return ResponseEntity.ok(dto);
    }
}

// DTOs
public record CreateCustomerRequestDto(
    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must not exceed 50 characters")
    String firstName,
    
    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    String lastName,
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,
    
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number must be valid")
    String phoneNumber,
    
    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    LocalDate dateOfBirth,
    
    @Valid
    AddressDto address
) {
    public record AddressDto(
        @NotBlank String street,
        @NotBlank String city,
        @NotBlank String state,
        @NotBlank String postalCode,
        @NotBlank String country,
        @NotBlank String type
    ) {}
}

public record CustomerResponseDto(
    UUID id,
    String firstName,
    String lastName,
    String email,
    String phoneNumber,
    LocalDate dateOfBirth,
    CustomerStatus status,
    List<AddressDto> addresses,
    Instant createdAt,
    Instant updatedAt
) {
    public record AddressDto(
        String street,
        String city,
        String state,
        String postalCode,
        String country,
        String type
    ) {}
}
```

## 3. Event Storming and Domain Modeling

### 3.1 Domain Events and Event Sourcing Patterns

```java
// Event Store Pattern Implementation
package com.enterprise.ecommerce.eventstore;

@Entity
@Table(name = "event_store")
public class EventStoreEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;
    
    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;
    
    @Column(name = "event_type", nullable = false)
    private String eventType;
    
    @Column(name = "event_data", nullable = false, columnDefinition = "TEXT")
    private String eventData;
    
    @Column(name = "event_version", nullable = false)
    private Long eventVersion;
    
    @Column(name = "occurred_on", nullable = false)
    private Instant occurredOn;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    // Constructors, getters, setters...
}

@Repository
public interface EventStoreRepository extends JpaRepository<EventStoreEntry, UUID> {
    List<EventStoreEntry> findByAggregateIdOrderByEventVersionAsc(UUID aggregateId);
    List<EventStoreEntry> findByAggregateTypeAndOccurredOnAfterOrderByOccurredOnAsc(
        String aggregateType, Instant after);
}

@Service
@Transactional
public class EventStore {
    private final EventStoreRepository repository;
    private final ObjectMapper objectMapper;
    private final DomainEventPublisher eventPublisher;
    
    public EventStore(EventStoreRepository repository,
                     ObjectMapper objectMapper,
                     DomainEventPublisher eventPublisher) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }
    
    public void saveEvents(UUID aggregateId, String aggregateType, 
                          List<DomainEvent> events, Long expectedVersion) {
        
        // Optimistic concurrency control
        var currentVersion = getCurrentVersion(aggregateId);
        if (!Objects.equals(currentVersion, expectedVersion)) {
            throw new ConcurrencyException(
                "Expected version " + expectedVersion + " but was " + currentVersion);
        }
        
        var entries = new ArrayList<EventStoreEntry>();
        for (int i = 0; i < events.size(); i++) {
            var event = events.get(i);
            var entry = new EventStoreEntry();
            entry.setAggregateId(aggregateId);
            entry.setAggregateType(aggregateType);
            entry.setEventType(event.getClass().getSimpleName());
            entry.setEventData(serializeEvent(event));
            entry.setEventVersion(expectedVersion + i + 1);
            entry.setOccurredOn(event.occurredOn());
            entry.setCreatedAt(Instant.now());
            
            entries.add(entry);
        }
        
        repository.saveAll(entries);
        
        // Publish events asynchronously
        events.forEach(eventPublisher::publish);
    }
    
    public List<DomainEvent> getEvents(UUID aggregateId) {
        return repository.findByAggregateIdOrderByEventVersionAsc(aggregateId)
            .stream()
            .map(this::deserializeEvent)
            .collect(Collectors.toList());
    }
    
    public Stream<DomainEvent> getEventsSince(String aggregateType, Instant since) {
        return repository.findByAggregateTypeAndOccurredOnAfterOrderByOccurredOnAsc(
                aggregateType, since)
            .stream()
            .map(this::deserializeEvent);
    }
    
    private Long getCurrentVersion(UUID aggregateId) {
        return repository.findByAggregateIdOrderByEventVersionAsc(aggregateId)
            .stream()
            .mapToLong(EventStoreEntry::getEventVersion)
            .max()
            .orElse(0L);
    }
    
    private String serializeEvent(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new EventSerializationException("Failed to serialize event", e);
        }
    }
    
    private DomainEvent deserializeEvent(EventStoreEntry entry) {
        try {
            var eventClass = Class.forName(getEventClassName(entry.getEventType()));
            return (DomainEvent) objectMapper.readValue(entry.getEventData(), eventClass);
        } catch (Exception e) {
            throw new EventDeserializationException("Failed to deserialize event", e);
        }
    }
    
    private String getEventClassName(String eventType) {
        // Map event type to full class name
        return "com.enterprise.ecommerce.customer.domain." + eventType;
    }
}
```

### 3.2 CQRS Implementation with Event Sourcing

```java
// Command Side - Write Model
@Component
public class CustomerCommandHandler {
    private final CustomerEventRepository eventRepository;
    private final EventStore eventStore;
    
    public CustomerCommandHandler(CustomerEventRepository eventRepository,
                                 EventStore eventStore) {
        this.eventRepository = eventRepository;
        this.eventStore = eventStore;
    }
    
    @CommandHandler
    public void handle(CreateCustomerCommand command) {
        // Load existing events to check for duplicates
        var existingEvents = eventStore.getEventsSince("Customer", Instant.EPOCH)
            .filter(event -> event instanceof CustomerCreatedEvent)
            .map(event -> (CustomerCreatedEvent) event)
            .filter(event -> event.email().equals(new Email(command.email())))
            .findAny();
            
        if (existingEvents.isPresent()) {
            throw new DuplicateCustomerException("Customer with email already exists");
        }
        
        // Create and save events
        var customerId = CustomerId.generate();
        var events = List.of(
            new CustomerCreatedEvent(customerId, new Email(command.email())),
            new CustomerProfileUpdatedEvent(
                customerId,
                null,
                new CustomerProfile(
                    command.firstName(),
                    command.lastName(),
                    new Email(command.email()),
                    new PhoneNumber(command.phoneNumber()),
                    command.dateOfBirth()
                )
            )
        );
        
        eventStore.saveEvents(customerId.value(), "Customer", events, 0L);
    }
    
    @CommandHandler
    public void handle(UpdateCustomerCommand command) {
        var customerId = command.customerId();
        var events = eventStore.getEvents(customerId.value());
        
        if (events.isEmpty()) {
            throw new CustomerNotFoundException("Customer not found: " + customerId);
        }
        
        // Rebuild aggregate from events
        var customer = CustomerAggregate.fromEvents(events);
        
        // Apply command
        customer.updateProfile(new CustomerProfile(
            command.firstName(),
            command.lastName(),
            new Email(command.email()),
            new PhoneNumber(command.phoneNumber()),
            command.dateOfBirth()
        ));
        
        // Save new events
        var newEvents = customer.getUncommittedEvents();
        var currentVersion = (long) events.size();
        
        eventStore.saveEvents(customerId.value(), "Customer", newEvents, currentVersion);
    }
}

// Query Side - Read Model
@Entity
@Table(name = "customer_read_model")
public class CustomerReadModel {
    @Id
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private CustomerStatus status;
    private String addressesJson;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
    
    // Getters, setters...
}

@Component
public class CustomerProjection {
    private final CustomerReadModelRepository readModelRepository;
    
    public CustomerProjection(CustomerReadModelRepository readModelRepository) {
        this.readModelRepository = readModelRepository;
    }
    
    @EventHandler
    public void on(CustomerCreatedEvent event) {
        var readModel = new CustomerReadModel();
        readModel.setId(event.customerId().value());
        readModel.setEmail(event.email().value());
        readModel.setCreatedAt(event.occurredOn());
        readModel.setVersion(1L);
        
        readModelRepository.save(readModel);
    }
    
    @EventHandler
    public void on(CustomerProfileUpdatedEvent event) {
        var readModel = readModelRepository.findById(event.customerId().value())
            .orElseThrow(() -> new IllegalStateException("Read model not found"));
            
        readModel.setFirstName(event.newProfile().firstName());
        readModel.setLastName(event.newProfile().lastName());
        readModel.setEmail(event.newProfile().email().value());
        readModel.setPhoneNumber(event.newProfile().phoneNumber().value());
        readModel.setDateOfBirth(event.newProfile().dateOfBirth());
        readModel.setUpdatedAt(event.occurredOn());
        readModel.setVersion(readModel.getVersion() + 1);
        
        readModelRepository.save(readModel);
    }
}
```

## 4. Testing Enterprise Architecture

### 4.1 Domain Testing Strategies

```java
// Domain Unit Tests
@ExtendWith(MockitoExtension.class)
class CustomerTest {
    
    @Test
    void shouldCreateCustomerWithValidProfile() {
        // Given
        var customerId = CustomerId.generate();
        var profile = new CustomerProfile(
            "John",
            "Doe",
            new Email("john.doe@example.com"),
            new PhoneNumber("+1234567890"),
            LocalDate.of(1990, 1, 1)
        );
        
        // When
        var customer = new Customer(customerId, profile);
        
        // Then
        assertThat(customer.getCustomerId()).isEqualTo(customerId);
        assertThat(customer.getProfile()).isEqualTo(profile);
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(customer.getDomainEvents()).hasSize(1);
        assertThat(customer.getDomainEvents().get(0))
            .isInstanceOf(CustomerCreatedEvent.class);
    }
    
    @Test
    void shouldAddAddressWhenValidAddress() {
        // Given
        var customer = createValidCustomer();
        var address = new Address(
            "123 Main St",
            "Anytown",
            "CA",
            "12345",
            "USA",
            AddressType.HOME
        );
        
        // When
        customer.addAddress(address);
        
        // Then
        assertThat(customer.getAddresses()).contains(address);
        assertThat(customer.getDomainEvents()).hasSize(2);
        assertThat(customer.getDomainEvents().get(1))
            .isInstanceOf(CustomerAddressAddedEvent.class);
    }
    
    @Test
    void shouldThrowExceptionWhenAddingTooManyAddresses() {
        // Given
        var customer = createValidCustomer();
        IntStream.range(0, 5).forEach(i -> 
            customer.addAddress(createValidAddress()));
        
        // When & Then
        assertThatThrownBy(() -> customer.addAddress(createValidAddress()))
            .isInstanceOf(CustomerBusinessException.class)
            .hasMessage("Customer cannot have more than 5 addresses");
    }
    
    @Test
    void shouldDeactivateCustomerWithReason() {
        // Given
        var customer = createValidCustomer();
        var reason = "Account closed by user request";
        
        // When
        customer.deactivate(reason);
        
        // Then
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.INACTIVE);
        assertThat(customer.getDomainEvents()).hasSize(2);
        
        var deactivationEvent = (CustomerDeactivatedEvent) customer.getDomainEvents().get(1);
        assertThat(deactivationEvent.reason()).isEqualTo(reason);
    }
    
    private Customer createValidCustomer() {
        return new Customer(
            CustomerId.generate(),
            new CustomerProfile(
                "John",
                "Doe",
                new Email("john.doe@example.com"),
                new PhoneNumber("+1234567890"),
                LocalDate.of(1990, 1, 1)
            )
        );
    }
    
    private Address createValidAddress() {
        return new Address(
            "123 Main St",
            "Anytown",
            "CA",
            "12345",
            "USA",
            AddressType.HOME
        );
    }
}

// Architecture Tests with ArchUnit
@AnalyzeClasses(packages = "com.enterprise.ecommerce")
class ArchitectureTest {
    
    @ArchTest
    static final ArchRule domainShouldNotDependOnInfrastructure =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("..infrastructure..");
    
    @ArchTest
    static final ArchRule applicationShouldNotDependOnWeb =
        noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAPackage("..web..");
    
    @ArchTest
    static final ArchRule entitiesShouldBeInDomainPackage =
        classes()
            .that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage("..domain..");
    
    @ArchTest
    static final ArchRule repositoriesShouldBeInterfaces =
        classes()
            .that().haveNameMatching(".*Repository")
            .and().resideInAPackage("..domain..")
            .should().beInterfaces();
    
    @ArchTest
    static final ArchRule useCasesShouldBeAnnotatedWithUseCase =
        classes()
            .that().haveNameMatching(".*UseCase")
            .should().beAnnotatedWith(UseCase.class);
    
    @ArchTest
    static final ArchRule domainEventsShouldImplementDomainEvent =
        classes()
            .that().areAnnotatedWith(DomainEvent.class)
            .should().implement(DomainEvent.class);
}
```

### 4.2 Integration Testing

```java
// Integration Test with Testcontainers
@SpringBootTest
@Testcontainers
@Transactional
class CustomerIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Autowired
    private CreateCustomerUseCase createCustomerUseCase;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Test
    void shouldCreateCustomerEndToEnd() {
        // Given
        var command = new CreateCustomerCommand(
            "John",
            "Doe",
            "john.doe@example.com",
            "+1234567890",
            LocalDate.of(1990, 1, 1),
            new CreateCustomerCommand.AddressDto(
                "123 Main St",
                "Anytown",
                "CA",
                "12345",
                "USA",
                "HOME"
            )
        );
        
        // When
        var response = createCustomerUseCase.execute(command);
        
        // Then
        assertThat(response.customerId()).isNotNull();
        assertThat(response.profile().firstName()).isEqualTo("John");
        assertThat(response.profile().email().value()).isEqualTo("john.doe@example.com");
        
        // Verify persistence
        var savedCustomer = customerRepository.findById(response.customerId());
        assertThat(savedCustomer).isPresent();
        assertThat(savedCustomer.get().getAddresses()).hasSize(1);
    }
}
```

## 5. Practical Exercises

### Exercise 1: Implement Order Management Bounded Context
Create a complete Order Management bounded context following DDD principles:
- Order aggregate with business rules
- Order repository interface
- Order creation use case
- Domain events for order state changes

### Exercise 2: Extend Hexagonal Architecture
Add a messaging adapter to the customer service:
- Implement event publishing via messaging
- Create event handlers for integration events
- Add retry and error handling mechanisms

### Exercise 3: Event Sourcing Implementation
Implement event sourcing for the Order aggregate:
- Create event store for order events
- Implement aggregate reconstruction from events
- Add snapshots for performance optimization

## 6. Assessment Questions

1. **Architectural Design**: Design a bounded context for a complex domain with multiple aggregates. Explain your design decisions and trade-offs.

2. **Hexagonal Architecture**: Implement a complete hexagonal architecture for a service including domain, application, and infrastructure layers.

3. **Domain Events**: Design and implement a domain event system with eventual consistency between bounded contexts.

4. **Testing Strategy**: Create a comprehensive testing strategy for a DDD-based application including unit, integration, and architecture tests.

## Summary

Today you learned advanced enterprise architecture patterns including:

- **Domain-Driven Design**: Strategic design, bounded contexts, and tactical patterns
- **Hexagonal Architecture**: Ports and adapters pattern with clean separation of concerns
- **Event Storming**: Domain modeling and event-driven design techniques
- **CQRS and Event Sourcing**: Advanced patterns for complex business domains
- **Testing Strategies**: Comprehensive testing approaches for enterprise architectures

These patterns form the foundation for building maintainable, scalable enterprise Java applications that can evolve with changing business requirements.

## Next Steps

Tomorrow, we'll explore **Advanced Resilience Engineering** focusing on circuit breakers, bulkheads, chaos engineering, and building self-healing systems that can handle failures gracefully in distributed environments.