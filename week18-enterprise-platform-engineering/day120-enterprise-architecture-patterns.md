# Day 120: Enterprise Architecture Patterns - Domain-Driven Design, Hexagonal Architecture & Clean Architecture

## Overview
Day 120 explores enterprise architecture patterns that form the foundation of large-scale, maintainable systems. We'll cover Domain-Driven Design (DDD), Hexagonal Architecture, Clean Architecture, and advanced CQRS patterns that enable building complex enterprise platforms.

## Learning Objectives
- Master Domain-Driven Design with bounded contexts and aggregates
- Implement Hexagonal Architecture for testable, flexible systems
- Apply Clean Architecture principles for maintainable codebases
- Design advanced CQRS systems with event sourcing at enterprise scale
- Build modular, decoupled architectures for complex business domains

## Key Concepts

### 1. Domain-Driven Design (DDD)
- Strategic design with bounded contexts
- Tactical patterns: Aggregates, Entities, Value Objects
- Domain services and application services
- Event storming and context mapping
- Ubiquitous language and domain modeling

### 2. Hexagonal Architecture (Ports & Adapters)
- Separation of core business logic from external concerns
- Primary and secondary adapters
- Dependency inversion at architectural level
- Testing strategies for hexagonal systems
- Port definitions and adapter implementations

### 3. Clean Architecture
- Dependency rule and layer separation
- Use cases and interactors
- Interface adapters and frameworks
- Independent deployment and testing
- Enterprise business rules vs application rules

## Implementation

### Domain-Driven Design Implementation

```java
// Strategic DDD: Bounded Context Definition
@BoundedContext("OrderManagement")
public class OrderManagementContext {
    
    // Aggregate Root
    @AggregateRoot
    @Entity
    public class Order {
        @AggregateId
        private OrderId orderId;
        private CustomerId customerId;
        private OrderStatus status;
        private Money totalAmount;
        private List<OrderLine> orderLines;
        private List<DomainEvent> domainEvents;
        
        // Factory method for creating orders
        public static Order createOrder(CustomerId customerId, List<OrderLineRequest> requests) {
            Order order = new Order();
            order.orderId = OrderId.generate();
            order.customerId = customerId;
            order.status = OrderStatus.PENDING;
            order.orderLines = new ArrayList<>();
            order.domainEvents = new ArrayList<>();
            
            // Apply business rules
            for (OrderLineRequest request : requests) {
                order.addOrderLine(request.getProductId(), request.getQuantity(), request.getPrice());
            }
            
            // Raise domain event
            order.addDomainEvent(new OrderCreatedEvent(order.orderId, order.customerId, order.totalAmount));
            
            return order;
        }
        
        // Business behavior
        public void addOrderLine(ProductId productId, Quantity quantity, Money unitPrice) {
            if (status != OrderStatus.PENDING) {
                throw new OrderNotModifiableException("Cannot modify order in status: " + status);
            }
            
            OrderLine orderLine = new OrderLine(productId, quantity, unitPrice);
            orderLines.add(orderLine);
            recalculateTotal();
            
            addDomainEvent(new OrderLineAddedEvent(orderId, productId, quantity, unitPrice));
        }
        
        public void confirmOrder() {
            if (status != OrderStatus.PENDING) {
                throw new OrderStatusException("Order cannot be confirmed from status: " + status);
            }
            
            if (orderLines.isEmpty()) {
                throw new EmptyOrderException("Cannot confirm empty order");
            }
            
            this.status = OrderStatus.CONFIRMED;
            addDomainEvent(new OrderConfirmedEvent(orderId, totalAmount));
        }
        
        public void cancel(String reason) {
            if (status == OrderStatus.DELIVERED || status == OrderStatus.CANCELLED) {
                throw new OrderStatusException("Order cannot be cancelled from status: " + status);
            }
            
            this.status = OrderStatus.CANCELLED;
            addDomainEvent(new OrderCancelledEvent(orderId, reason));
        }
        
        private void recalculateTotal() {
            this.totalAmount = orderLines.stream()
                .map(OrderLine::getLineTotal)
                .reduce(Money.ZERO, Money::add);
        }
        
        private void addDomainEvent(DomainEvent event) {
            if (domainEvents == null) {
                domainEvents = new ArrayList<>();
            }
            domainEvents.add(event);
        }
        
        public List<DomainEvent> getUncommittedEvents() {
            return new ArrayList<>(domainEvents);
        }
        
        public void markEventsAsCommitted() {
            domainEvents.clear();
        }
    }
    
    // Value Object
    @ValueObject
    public static class OrderLine {
        private final ProductId productId;
        private final Quantity quantity;
        private final Money unitPrice;
        private final Money lineTotal;
        
        public OrderLine(ProductId productId, Quantity quantity, Money unitPrice) {
            this.productId = Objects.requireNonNull(productId);
            this.quantity = Objects.requireNonNull(quantity);
            this.unitPrice = Objects.requireNonNull(unitPrice);
            this.lineTotal = unitPrice.multiply(quantity.getValue());
        }
        
        // Value objects are immutable and equality is based on values
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OrderLine orderLine = (OrderLine) o;
            return Objects.equals(productId, orderLine.productId) &&
                   Objects.equals(quantity, orderLine.quantity) &&
                   Objects.equals(unitPrice, orderLine.unitPrice);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(productId, quantity, unitPrice);
        }
        
        // Getters
        public ProductId getProductId() { return productId; }
        public Quantity getQuantity() { return quantity; }
        public Money getUnitPrice() { return unitPrice; }
        public Money getLineTotal() { return lineTotal; }
    }
    
    // Domain Service
    @DomainService
    public class OrderPricingService {
        
        private final PricingRepository pricingRepository;
        private final DiscountPolicy discountPolicy;
        
        public OrderPricingService(PricingRepository pricingRepository, DiscountPolicy discountPolicy) {
            this.pricingRepository = pricingRepository;
            this.discountPolicy = discountPolicy;
        }
        
        public Money calculateOrderTotal(Order order) {
            Money subtotal = order.getOrderLines().stream()
                .map(this::calculateLinePrice)
                .reduce(Money.ZERO, Money::add);
            
            Discount discount = discountPolicy.calculateDiscount(order);
            return subtotal.subtract(discount.getAmount());
        }
        
        private Money calculateLinePrice(OrderLine orderLine) {
            ProductPrice currentPrice = pricingRepository.getCurrentPrice(orderLine.getProductId());
            return currentPrice.getPrice().multiply(orderLine.getQuantity().getValue());
        }
    }
    
    // Repository Interface (Port)
    public interface OrderRepository {
        Order findById(OrderId orderId);
        void save(Order order);
        List<Order> findByCustomerId(CustomerId customerId);
        List<Order> findByStatus(OrderStatus status);
    }
    
    // Application Service
    @ApplicationService
    @Transactional
    public class OrderApplicationService {
        
        private final OrderRepository orderRepository;
        private final OrderPricingService pricingService;
        private final DomainEventPublisher eventPublisher;
        
        public OrderApplicationService(OrderRepository orderRepository, 
                                     OrderPricingService pricingService,
                                     DomainEventPublisher eventPublisher) {
            this.orderRepository = orderRepository;
            this.pricingService = pricingService;
            this.eventPublisher = eventPublisher;
        }
        
        public OrderId createOrder(CreateOrderCommand command) {
            // Create order using domain factory
            Order order = Order.createOrder(command.getCustomerId(), command.getOrderLines());
            
            // Apply domain service for pricing
            Money calculatedTotal = pricingService.calculateOrderTotal(order);
            
            // Save aggregate
            orderRepository.save(order);
            
            // Publish domain events
            publishDomainEvents(order);
            
            return order.getOrderId();
        }
        
        public void confirmOrder(ConfirmOrderCommand command) {
            Order order = orderRepository.findById(command.getOrderId());
            if (order == null) {
                throw new OrderNotFoundException(command.getOrderId());
            }
            
            order.confirmOrder();
            orderRepository.save(order);
            
            publishDomainEvents(order);
        }
        
        public void cancelOrder(CancelOrderCommand command) {
            Order order = orderRepository.findById(command.getOrderId());
            if (order == null) {
                throw new OrderNotFoundException(command.getOrderId());
            }
            
            order.cancel(command.getReason());
            orderRepository.save(order);
            
            publishDomainEvents(order);
        }
        
        private void publishDomainEvents(Order order) {
            order.getUncommittedEvents().forEach(eventPublisher::publish);
            order.markEventsAsCommitted();
        }
    }
}

// Supporting Value Objects and Entities
@ValueObject
public class OrderId {
    private final String value;
    
    private OrderId(String value) {
        this.value = Objects.requireNonNull(value);
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

@ValueObject
public class Money {
    public static final Money ZERO = new Money(BigDecimal.ZERO, Currency.getInstance("USD"));
    
    private final BigDecimal amount;
    private final Currency currency;
    
    public Money(BigDecimal amount, Currency currency) {
        this.amount = Objects.requireNonNull(amount);
        this.currency = Objects.requireNonNull(currency);
    }
    
    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new CurrencyMismatchException("Cannot add different currencies");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }
    
    public Money subtract(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new CurrencyMismatchException("Cannot subtract different currencies");
        }
        return new Money(this.amount.subtract(other.amount), this.currency);
    }
    
    public Money multiply(int multiplier) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)), this.currency);
    }
    
    // Equals, hashCode, toString methods
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return Objects.equals(amount, money.amount) && Objects.equals(currency, money.currency);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }
    
    public BigDecimal getAmount() { return amount; }
    public Currency getCurrency() { return currency; }
}

enum OrderStatus {
    PENDING, CONFIRMED, IN_PROGRESS, DELIVERED, CANCELLED
}
```

### Hexagonal Architecture Implementation

```java
// Core Domain (Inside the Hexagon)
@Component
public class OrderProcessingService {
    
    // Primary Ports (driven by external actors)
    public interface OrderManagementPort {
        OrderResult processOrder(OrderRequest request);
        OrderStatus getOrderStatus(String orderId);
        void cancelOrder(String orderId, String reason);
    }
    
    // Secondary Ports (driving external systems)
    public interface PaymentPort {
        PaymentResult processPayment(PaymentRequest request);
        void refundPayment(String paymentId, Money amount);
    }
    
    public interface InventoryPort {
        boolean checkAvailability(ProductId productId, int quantity);
        void reserveItems(List<ReservationRequest> reservations);
        void releaseReservation(String reservationId);
    }
    
    public interface NotificationPort {
        void sendOrderConfirmation(String customerId, OrderDetails details);
        void sendOrderStatusUpdate(String customerId, String orderId, OrderStatus status);
    }
    
    public interface OrderPersistencePort {
        void saveOrder(Order order);
        Order loadOrder(String orderId);
        List<Order> findOrdersByCustomer(String customerId);
    }
    
    // Core Business Logic
    @Service
    public class OrderProcessingServiceImpl implements OrderManagementPort {
        
        private final PaymentPort paymentPort;
        private final InventoryPort inventoryPort;
        private final NotificationPort notificationPort;
        private final OrderPersistencePort persistencePort;
        
        public OrderProcessingServiceImpl(PaymentPort paymentPort,
                                        InventoryPort inventoryPort,
                                        NotificationPort notificationPort,
                                        OrderPersistencePort persistencePort) {
            this.paymentPort = paymentPort;
            this.inventoryPort = inventoryPort;
            this.notificationPort = notificationPort;
            this.persistencePort = persistencePort;
        }
        
        @Override
        @Transactional
        public OrderResult processOrder(OrderRequest request) {
            try {
                // 1. Validate order
                validateOrder(request);
                
                // 2. Check inventory availability
                if (!checkInventoryAvailability(request)) {
                    return OrderResult.failed("Insufficient inventory");
                }
                
                // 3. Reserve inventory
                String reservationId = reserveInventory(request);
                
                // 4. Process payment
                PaymentResult paymentResult = processPayment(request);
                if (!paymentResult.isSuccess()) {
                    // Rollback inventory reservation
                    inventoryPort.releaseReservation(reservationId);
                    return OrderResult.failed("Payment failed: " + paymentResult.getErrorMessage());
                }
                
                // 5. Create and save order
                Order order = createOrder(request, paymentResult, reservationId);
                persistencePort.saveOrder(order);
                
                // 6. Send confirmation
                notificationPort.sendOrderConfirmation(request.getCustomerId(), 
                    new OrderDetails(order));
                
                return OrderResult.success(order.getOrderId());
                
            } catch (Exception e) {
                log.error("Order processing failed", e);
                return OrderResult.failed("Order processing failed: " + e.getMessage());
            }
        }
        
        @Override
        public OrderStatus getOrderStatus(String orderId) {
            Order order = persistencePort.loadOrder(orderId);
            return order != null ? order.getStatus() : null;
        }
        
        @Override
        @Transactional
        public void cancelOrder(String orderId, String reason) {
            Order order = persistencePort.loadOrder(orderId);
            if (order == null) {
                throw new OrderNotFoundException(orderId);
            }
            
            if (order.canBeCancelled()) {
                // Release inventory
                inventoryPort.releaseReservation(order.getReservationId());
                
                // Refund payment if applicable
                if (order.getPaymentId() != null) {
                    paymentPort.refundPayment(order.getPaymentId(), order.getTotalAmount());
                }
                
                // Update order status
                order.cancel(reason);
                persistencePort.saveOrder(order);
                
                // Notify customer
                notificationPort.sendOrderStatusUpdate(order.getCustomerId(), orderId, OrderStatus.CANCELLED);
            } else {
                throw new OrderCancellationException("Order cannot be cancelled in current status: " + order.getStatus());
            }
        }
        
        // Private helper methods
        private void validateOrder(OrderRequest request) {
            if (request.getOrderLines().isEmpty()) {
                throw new InvalidOrderException("Order must contain at least one item");
            }
            
            for (OrderLineRequest line : request.getOrderLines()) {
                if (line.getQuantity() <= 0) {
                    throw new InvalidOrderException("Order line quantity must be positive");
                }
                if (line.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new InvalidOrderException("Order line price must be positive");
                }
            }
        }
        
        private boolean checkInventoryAvailability(OrderRequest request) {
            return request.getOrderLines().stream()
                .allMatch(line -> inventoryPort.checkAvailability(line.getProductId(), line.getQuantity()));
        }
        
        private String reserveInventory(OrderRequest request) {
            List<ReservationRequest> reservations = request.getOrderLines().stream()
                .map(line -> new ReservationRequest(line.getProductId(), line.getQuantity()))
                .collect(Collectors.toList());
            
            inventoryPort.reserveItems(reservations);
            return "RES-" + UUID.randomUUID().toString(); // Simplified
        }
        
        private PaymentResult processPayment(OrderRequest request) {
            Money totalAmount = calculateTotalAmount(request);
            PaymentRequest paymentRequest = new PaymentRequest(
                request.getCustomerId(), 
                totalAmount, 
                request.getPaymentMethod()
            );
            
            return paymentPort.processPayment(paymentRequest);
        }
        
        private Money calculateTotalAmount(OrderRequest request) {
            return request.getOrderLines().stream()
                .map(line -> Money.of(line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity()))))
                .reduce(Money.ZERO, Money::add);
        }
        
        private Order createOrder(OrderRequest request, PaymentResult paymentResult, String reservationId) {
            return Order.builder()
                .orderId(UUID.randomUUID().toString())
                .customerId(request.getCustomerId())
                .orderLines(request.getOrderLines())
                .status(OrderStatus.CONFIRMED)
                .totalAmount(calculateTotalAmount(request))
                .paymentId(paymentResult.getPaymentId())
                .reservationId(reservationId)
                .createdAt(Instant.now())
                .build();
        }
    }
}

// Primary Adapters (Controllers/API)
@RestController
@RequestMapping("/api/orders")
@Validated
public class OrderController {
    
    private final OrderProcessingService.OrderManagementPort orderManagementPort;
    
    public OrderController(OrderProcessingService.OrderManagementPort orderManagementPort) {
        this.orderManagementPort = orderManagementPort;
    }
    
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderRequest orderRequest = mapToOrderRequest(request);
        OrderResult result = orderManagementPort.processOrder(orderRequest);
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(new OrderResponse(result.getOrderId(), "Order created successfully"));
        } else {
            return ResponseEntity.badRequest().body(new OrderResponse(null, result.getErrorMessage()));
        }
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderStatusResponse> getOrderStatus(@PathVariable String orderId) {
        OrderStatus status = orderManagementPort.getOrderStatus(orderId);
        
        if (status != null) {
            return ResponseEntity.ok(new OrderStatusResponse(orderId, status));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> cancelOrder(@PathVariable String orderId, 
                                          @RequestBody CancelOrderRequest request) {
        try {
            orderManagementPort.cancelOrder(orderId, request.getReason());
            return ResponseEntity.ok().build();
        } catch (OrderNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (OrderCancellationException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    private OrderRequest mapToOrderRequest(CreateOrderRequest request) {
        return OrderRequest.builder()
            .customerId(request.getCustomerId())
            .orderLines(request.getItems().stream()
                .map(item -> new OrderLineRequest(item.getProductId(), item.getQuantity(), item.getPrice()))
                .collect(Collectors.toList()))
            .paymentMethod(request.getPaymentMethod())
            .build();
    }
}

// Secondary Adapters (Infrastructure)
@Component
public class DatabaseOrderPersistenceAdapter implements OrderProcessingService.OrderPersistencePort {
    
    private final OrderJpaRepository orderRepository;
    private final OrderMapper orderMapper;
    
    public DatabaseOrderPersistenceAdapter(OrderJpaRepository orderRepository, OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
    }
    
    @Override
    public void saveOrder(Order order) {
        OrderEntity entity = orderMapper.toEntity(order);
        orderRepository.save(entity);
    }
    
    @Override
    public Order loadOrder(String orderId) {
        return orderRepository.findById(orderId)
            .map(orderMapper::toDomain)
            .orElse(null);
    }
    
    @Override
    public List<Order> findOrdersByCustomer(String customerId) {
        return orderRepository.findByCustomerId(customerId)
            .stream()
            .map(orderMapper::toDomain)
            .collect(Collectors.toList());
    }
}

@Component
public class PaymentServiceAdapter implements OrderProcessingService.PaymentPort {
    
    private final PaymentServiceClient paymentClient;
    
    public PaymentServiceAdapter(PaymentServiceClient paymentClient) {
        this.paymentClient = paymentClient;
    }
    
    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        try {
            PaymentResponse response = paymentClient.processPayment(
                request.getCustomerId(),
                request.getAmount(),
                request.getPaymentMethod()
            );
            
            return PaymentResult.builder()
                .success(response.isSuccess())
                .paymentId(response.getPaymentId())
                .errorMessage(response.getErrorMessage())
                .build();
                
        } catch (Exception e) {
            return PaymentResult.builder()
                .success(false)
                .errorMessage("Payment service unavailable: " + e.getMessage())
                .build();
        }
    }
    
    @Override
    public void refundPayment(String paymentId, Money amount) {
        paymentClient.refundPayment(paymentId, amount.getAmount());
    }
}
```

### Clean Architecture Implementation

```java
// Enterprise Business Rules (Entities)
public class User {
    private final UserId id;
    private String email;
    private String name;
    private boolean active;
    
    public User(UserId id, String email, String name) {
        this.id = Objects.requireNonNull(id);
        this.email = validateEmail(email);
        this.name = validateName(name);
        this.active = true;
    }
    
    // Enterprise business rules
    public void changeEmail(String newEmail) {
        String validatedEmail = validateEmail(newEmail);
        this.email = validatedEmail;
    }
    
    public void deactivate() {
        this.active = false;
    }
    
    public void activate() {
        this.active = true;
    }
    
    private String validateEmail(String email) {
        if (email == null || !email.contains("@")) {
            throw new InvalidEmailException("Invalid email format");
        }
        return email;
    }
    
    private String validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidNameException("Name cannot be empty");
        }
        return name.trim();
    }
    
    // Getters
    public UserId getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
}

// Application Business Rules (Use Cases)
public interface CreateUserUseCase {
    CreateUserResponse execute(CreateUserRequest request);
}

public interface GetUserUseCase {
    GetUserResponse execute(GetUserRequest request);
}

public interface UpdateUserUseCase {
    UpdateUserResponse execute(UpdateUserRequest request);
}

// Use Case Implementation
@Component
public class CreateUserUseCaseImpl implements CreateUserUseCase {
    
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final UserPresenter userPresenter;
    
    public CreateUserUseCaseImpl(UserRepository userRepository, 
                               EmailService emailService,
                               UserPresenter userPresenter) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.userPresenter = userPresenter;
    }
    
    @Override
    public CreateUserResponse execute(CreateUserRequest request) {
        // Business logic
        if (userRepository.existsByEmail(request.getEmail())) {
            return userPresenter.presentUserAlreadyExists(request.getEmail());
        }
        
        // Create user entity
        UserId userId = UserId.generate();
        User user = new User(userId, request.getEmail(), request.getName());
        
        // Save user
        userRepository.save(user);
        
        // Send welcome email (side effect)
        emailService.sendWelcomeEmail(user.getEmail(), user.getName());
        
        // Present result
        return userPresenter.presentUserCreated(user);
    }
}

@Component
public class GetUserUseCaseImpl implements GetUserUseCase {
    
    private final UserRepository userRepository;
    private final UserPresenter userPresenter;
    
    public GetUserUseCaseImpl(UserRepository userRepository, UserPresenter userPresenter) {
        this.userRepository = userRepository;
        this.userPresenter = userPresenter;
    }
    
    @Override
    public GetUserResponse execute(GetUserRequest request) {
        User user = userRepository.findById(request.getUserId());
        
        if (user == null) {
            return userPresenter.presentUserNotFound(request.getUserId());
        }
        
        return userPresenter.presentUser(user);
    }
}

// Interface Adapters Layer
public interface UserRepository {
    void save(User user);
    User findById(UserId userId);
    User findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findAll();
}

public interface EmailService {
    void sendWelcomeEmail(String email, String name);
    void sendPasswordResetEmail(String email, String resetToken);
}

public interface UserPresenter {
    CreateUserResponse presentUserCreated(User user);
    CreateUserResponse presentUserAlreadyExists(String email);
    GetUserResponse presentUser(User user);
    GetUserResponse presentUserNotFound(UserId userId);
}

// Controller (Interface Adapter)
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private final CreateUserUseCase createUserUseCase;
    private final GetUserUseCase getUserUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    
    public UserController(CreateUserUseCase createUserUseCase,
                         GetUserUseCase getUserUseCase,
                         UpdateUserUseCase updateUserUseCase) {
        this.createUserUseCase = createUserUseCase;
        this.getUserUseCase = getUserUseCase;
        this.updateUserUseCase = updateUserUseCase;
    }
    
    @PostMapping
    public ResponseEntity<CreateUserResponseDto> createUser(@Valid @RequestBody CreateUserRequestDto requestDto) {
        CreateUserRequest request = new CreateUserRequest(requestDto.getEmail(), requestDto.getName());
        CreateUserResponse response = createUserUseCase.execute(request);
        
        CreateUserResponseDto responseDto = mapToDto(response);
        
        if (response.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
        } else {
            return ResponseEntity.badRequest().body(responseDto);
        }
    }
    
    @GetMapping("/{userId}")
    public ResponseEntity<GetUserResponseDto> getUser(@PathVariable String userId) {
        GetUserRequest request = new GetUserRequest(UserId.of(userId));
        GetUserResponse response = getUserUseCase.execute(request);
        
        GetUserResponseDto responseDto = mapToDto(response);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(responseDto);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PutMapping("/{userId}")
    public ResponseEntity<UpdateUserResponseDto> updateUser(@PathVariable String userId,
                                                           @Valid @RequestBody UpdateUserRequestDto requestDto) {
        UpdateUserRequest request = new UpdateUserRequest(
            UserId.of(userId), 
            requestDto.getEmail(), 
            requestDto.getName()
        );
        UpdateUserResponse response = updateUserUseCase.execute(request);
        
        UpdateUserResponseDto responseDto = mapToDto(response);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(responseDto);
        } else {
            return ResponseEntity.badRequest().body(responseDto);
        }
    }
    
    // Mapping methods
    private CreateUserResponseDto mapToDto(CreateUserResponse response) {
        return new CreateUserResponseDto(
            response.getUserId() != null ? response.getUserId().getValue() : null,
            response.isSuccess(),
            response.getErrorMessage()
        );
    }
    
    private GetUserResponseDto mapToDto(GetUserResponse response) {
        return new GetUserResponseDto(
            response.getUserId() != null ? response.getUserId().getValue() : null,
            response.getEmail(),
            response.getName(),
            response.isActive(),
            response.isSuccess(),
            response.getErrorMessage()
        );
    }
    
    private UpdateUserResponseDto mapToDto(UpdateUserResponse response) {
        return new UpdateUserResponseDto(
            response.getUserId() != null ? response.getUserId().getValue() : null,
            response.isSuccess(),
            response.getErrorMessage()
        );
    }
}

// Presenter Implementation
@Component
public class UserPresenterImpl implements UserPresenter {
    
    @Override
    public CreateUserResponse presentUserCreated(User user) {
        return CreateUserResponse.builder()
            .userId(user.getId())
            .success(true)
            .message("User created successfully")
            .build();
    }
    
    @Override
    public CreateUserResponse presentUserAlreadyExists(String email) {
        return CreateUserResponse.builder()
            .success(false)
            .errorMessage("User with email " + email + " already exists")
            .build();
    }
    
    @Override
    public GetUserResponse presentUser(User user) {
        return GetUserResponse.builder()
            .userId(user.getId())
            .email(user.getEmail())
            .name(user.getName())
            .active(user.isActive())
            .success(true)
            .build();
    }
    
    @Override
    public GetUserResponse presentUserNotFound(UserId userId) {
        return GetUserResponse.builder()
            .success(false)
            .errorMessage("User not found: " + userId.getValue())
            .build();
    }
}

// Infrastructure Layer
@Repository
public class JpaUserRepository implements UserRepository {
    
    private final UserJpaRepository jpaRepository;
    private final UserEntityMapper mapper;
    
    public JpaUserRepository(UserJpaRepository jpaRepository, UserEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    public void save(User user) {
        UserEntity entity = mapper.toEntity(user);
        jpaRepository.save(entity);
    }
    
    @Override
    public User findById(UserId userId) {
        return jpaRepository.findById(userId.getValue())
            .map(mapper::toDomain)
            .orElse(null);
    }
    
    @Override
    public User findByEmail(String email) {
        return jpaRepository.findByEmail(email)
            .map(mapper::toDomain)
            .orElse(null);
    }
    
    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }
    
    @Override
    public List<User> findAll() {
        return jpaRepository.findAll()
            .stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }
}

@Service
public class SmtpEmailService implements EmailService {
    
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    
    public SmtpEmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }
    
    @Override
    public void sendWelcomeEmail(String email, String name) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setTo(email);
            helper.setSubject("Welcome to Our Platform");
            
            Context context = new Context();
            context.setVariable("name", name);
            String htmlContent = templateEngine.process("welcome-email", context);
            
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            
        } catch (MessagingException e) {
            throw new EmailSendException("Failed to send welcome email", e);
        }
    }
    
    @Override
    public void sendPasswordResetEmail(String email, String resetToken) {
        // Implementation similar to welcome email
    }
}
```

## Advanced CQRS with Event Sourcing

```java
// Command Side
@Component
public class OrderCommandHandler {
    
    private final EventStore eventStore;
    private final OrderProjectionUpdater projectionUpdater;
    
    public OrderCommandHandler(EventStore eventStore, OrderProjectionUpdater projectionUpdater) {
        this.eventStore = eventStore;
        this.projectionUpdater = projectionUpdater;
    }
    
    @CommandHandler
    public void handle(CreateOrderCommand command) {
        // Load aggregate from event store
        OrderAggregate aggregate = loadAggregate(command.getOrderId());
        
        // Execute command
        aggregate.createOrder(command.getCustomerId(), command.getOrderLines());
        
        // Save events
        List<DomainEvent> uncommittedEvents = aggregate.getUncommittedEvents();
        eventStore.saveEvents(command.getOrderId(), uncommittedEvents, aggregate.getVersion());
        
        // Update projections asynchronously
        projectionUpdater.updateProjections(uncommittedEvents);
    }
    
    @CommandHandler
    public void handle(AddOrderLineCommand command) {
        OrderAggregate aggregate = loadAggregate(command.getOrderId());
        aggregate.addOrderLine(command.getProductId(), command.getQuantity(), command.getUnitPrice());
        
        List<DomainEvent> uncommittedEvents = aggregate.getUncommittedEvents();
        eventStore.saveEvents(command.getOrderId(), uncommittedEvents, aggregate.getVersion());
        
        projectionUpdater.updateProjections(uncommittedEvents);
    }
    
    private OrderAggregate loadAggregate(String orderId) {
        List<DomainEvent> events = eventStore.getEvents(orderId);
        return OrderAggregate.fromHistory(events);
    }
}

// Query Side
@Component
public class OrderQueryHandler {
    
    private final OrderReadModelRepository readModelRepository;
    
    public OrderQueryHandler(OrderReadModelRepository readModelRepository) {
        this.readModelRepository = readModelRepository;
    }
    
    @QueryHandler
    public OrderDetailsView handle(GetOrderDetailsQuery query) {
        return readModelRepository.findOrderDetails(query.getOrderId());
    }
    
    @QueryHandler
    public List<OrderSummaryView> handle(GetCustomerOrdersQuery query) {
        return readModelRepository.findOrdersByCustomer(query.getCustomerId());
    }
    
    @QueryHandler
    public OrderStatisticsView handle(GetOrderStatisticsQuery query) {
        return readModelRepository.getOrderStatistics(query.getFromDate(), query.getToDate());
    }
}

// Event Store Implementation
@Component
public class EventStoreImpl implements EventStore {
    
    private final EventStoreRepository repository;
    private final EventSerializer serializer;
    
    public EventStoreImpl(EventStoreRepository repository, EventSerializer serializer) {
        this.repository = repository;
        this.serializer = serializer;
    }
    
    @Override
    @Transactional
    public void saveEvents(String aggregateId, List<DomainEvent> events, long expectedVersion) {
        // Optimistic concurrency control
        long currentVersion = repository.getCurrentVersion(aggregateId);
        if (currentVersion != expectedVersion) {
            throw new ConcurrencyException("Aggregate version mismatch");
        }
        
        // Save events
        for (int i = 0; i < events.size(); i++) {
            DomainEvent event = events.get(i);
            EventStoreEntry entry = EventStoreEntry.builder()
                .aggregateId(aggregateId)
                .eventType(event.getClass().getSimpleName())
                .eventData(serializer.serialize(event))
                .version(currentVersion + i + 1)
                .timestamp(Instant.now())
                .build();
            
            repository.save(entry);
        }
    }
    
    @Override
    public List<DomainEvent> getEvents(String aggregateId) {
        return repository.findByAggregateIdOrderByVersion(aggregateId)
            .stream()
            .map(entry -> serializer.deserialize(entry.getEventData(), entry.getEventType()))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<DomainEvent> getEventsAfterVersion(String aggregateId, long version) {
        return repository.findByAggregateIdAndVersionGreaterThanOrderByVersion(aggregateId, version)
            .stream()
            .map(entry -> serializer.deserialize(entry.getEventData(), entry.getEventType()))
            .collect(Collectors.toList());
    }
}

// Projection Updater
@Component
public class OrderProjectionUpdater {
    
    private final OrderReadModelRepository readModelRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    public OrderProjectionUpdater(OrderReadModelRepository readModelRepository,
                                ApplicationEventPublisher eventPublisher) {
        this.readModelRepository = readModelRepository;
        this.eventPublisher = eventPublisher;
    }
    
    @Async
    public void updateProjections(List<DomainEvent> events) {
        for (DomainEvent event : events) {
            updateProjection(event);
            // Publish integration event for other bounded contexts
            eventPublisher.publishEvent(new ProjectionUpdatedEvent(event));
        }
    }
    
    private void updateProjection(DomainEvent event) {
        switch (event) {
            case OrderCreatedEvent orderCreated -> handleOrderCreated(orderCreated);
            case OrderLineAddedEvent lineAdded -> handleOrderLineAdded(lineAdded);
            case OrderConfirmedEvent orderConfirmed -> handleOrderConfirmed(orderConfirmed);
            case OrderCancelledEvent orderCancelled -> handleOrderCancelled(orderCancelled);
            default -> log.debug("No projection handler for event: {}", event.getClass().getSimpleName());
        }
    }
    
    private void handleOrderCreated(OrderCreatedEvent event) {
        OrderDetailsView orderView = OrderDetailsView.builder()
            .orderId(event.getOrderId())
            .customerId(event.getCustomerId())
            .status(OrderStatus.PENDING.name())
            .totalAmount(event.getTotalAmount())
            .createdAt(event.getTimestamp())
            .orderLines(new ArrayList<>())
            .build();
        
        readModelRepository.saveOrderDetails(orderView);
        
        // Update customer summary
        updateCustomerOrderSummary(event.getCustomerId());
    }
    
    private void handleOrderLineAdded(OrderLineAddedEvent event) {
        OrderDetailsView orderView = readModelRepository.findOrderDetails(event.getOrderId());
        if (orderView != null) {
            OrderLineView lineView = OrderLineView.builder()
                .productId(event.getProductId())
                .quantity(event.getQuantity())
                .unitPrice(event.getUnitPrice())
                .lineTotal(event.getUnitPrice().multiply(BigDecimal.valueOf(event.getQuantity())))
                .build();
            
            orderView.getOrderLines().add(lineView);
            orderView.setTotalAmount(calculateNewTotal(orderView.getOrderLines()));
            
            readModelRepository.saveOrderDetails(orderView);
        }
    }
    
    private void handleOrderConfirmed(OrderConfirmedEvent event) {
        updateOrderStatus(event.getOrderId(), OrderStatus.CONFIRMED.name());
    }
    
    private void handleOrderCancelled(OrderCancelledEvent event) {
        updateOrderStatus(event.getOrderId(), OrderStatus.CANCELLED.name());
    }
    
    private void updateOrderStatus(String orderId, String status) {
        OrderDetailsView orderView = readModelRepository.findOrderDetails(orderId);
        if (orderView != null) {
            orderView.setStatus(status);
            readModelRepository.saveOrderDetails(orderView);
        }
    }
    
    private void updateCustomerOrderSummary(String customerId) {
        List<OrderSummaryView> customerOrders = readModelRepository.findOrdersByCustomer(customerId);
        
        CustomerOrderSummary summary = CustomerOrderSummary.builder()
            .customerId(customerId)
            .totalOrders(customerOrders.size())
            .totalSpent(customerOrders.stream()
                .map(OrderSummaryView::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add))
            .lastOrderDate(customerOrders.stream()
                .map(OrderSummaryView::getCreatedAt)
                .max(Instant::compareTo)
                .orElse(null))
            .build();
        
        readModelRepository.saveCustomerSummary(summary);
    }
    
    private BigDecimal calculateNewTotal(List<OrderLineView> orderLines) {
        return orderLines.stream()
            .map(OrderLineView::getLineTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
```

## Testing Strategies for Enterprise Architecture

```java
// Domain Testing
@ExtendWith(MockitoExtension.class)
class OrderAggregateTest {
    
    @Test
    void shouldCreateOrderWithValidData() {
        // Given
        String customerId = "CUST-123";
        List<OrderLineRequest> orderLines = Arrays.asList(
            new OrderLineRequest("PROD-1", 2, BigDecimal.valueOf(10.00)),
            new OrderLineRequest("PROD-2", 1, BigDecimal.valueOf(25.00))
        );
        
        // When
        Order order = Order.createOrder(CustomerId.of(customerId), orderLines);
        
        // Then
        assertThat(order.getCustomerId().getValue()).isEqualTo(customerId);
        assertThat(order.getOrderLines()).hasSize(2);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getTotalAmount().getAmount()).isEqualTo(BigDecimal.valueOf(45.00));
        
        // Check domain events
        List<DomainEvent> events = order.getUncommittedEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(OrderCreatedEvent.class);
    }
    
    @Test
    void shouldThrowExceptionWhenConfirmingEmptyOrder() {
        // Given
        Order order = Order.createOrder(CustomerId.of("CUST-123"), Collections.emptyList());
        
        // When/Then
        assertThatThrownBy(order::confirmOrder)
            .isInstanceOf(EmptyOrderException.class)
            .hasMessage("Cannot confirm empty order");
    }
}

// Application Service Testing
@ExtendWith(MockitoExtension.class)
class OrderApplicationServiceTest {
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private OrderPricingService pricingService;
    
    @Mock
    private DomainEventPublisher eventPublisher;
    
    @InjectMocks
    private OrderApplicationService orderApplicationService;
    
    @Test
    void shouldCreateOrderSuccessfully() {
        // Given
        CreateOrderCommand command = CreateOrderCommand.builder()
            .customerId(CustomerId.of("CUST-123"))
            .orderLines(Arrays.asList(
                new OrderLineRequest("PROD-1", 2, BigDecimal.valueOf(10.00))
            ))
            .build();
        
        when(pricingService.calculateOrderTotal(any(Order.class)))
            .thenReturn(Money.of(BigDecimal.valueOf(20.00)));
        
        // When
        OrderId orderId = orderApplicationService.createOrder(command);
        
        // Then
        assertThat(orderId).isNotNull();
        verify(orderRepository).save(any(Order.class));
        verify(eventPublisher).publish(any(OrderCreatedEvent.class));
    }
}

// Hexagonal Architecture Testing
@ExtendWith(MockitoExtension.class)
class OrderProcessingServiceTest {
    
    @Mock
    private PaymentPort paymentPort;
    
    @Mock
    private InventoryPort inventoryPort;
    
    @Mock
    private NotificationPort notificationPort;
    
    @Mock
    private OrderPersistencePort persistencePort;
    
    @InjectMocks
    private OrderProcessingServiceImpl orderProcessingService;
    
    @Test
    void shouldProcessOrderSuccessfully() {
        // Given
        OrderRequest request = createValidOrderRequest();
        
        when(inventoryPort.checkAvailability(any(), anyInt())).thenReturn(true);
        when(paymentPort.processPayment(any())).thenReturn(PaymentResult.success("PAY-123"));
        
        // When
        OrderResult result = orderProcessingService.processOrder(request);
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrderId()).isNotNull();
        
        verify(inventoryPort).reserveItems(any());
        verify(paymentPort).processPayment(any());
        verify(persistencePort).saveOrder(any());
        verify(notificationPort).sendOrderConfirmation(any(), any());
    }
    
    @Test
    void shouldFailWhenInventoryUnavailable() {
        // Given
        OrderRequest request = createValidOrderRequest();
        when(inventoryPort.checkAvailability(any(), anyInt())).thenReturn(false);
        
        // When
        OrderResult result = orderProcessingService.processOrder(request);
        
        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Insufficient inventory");
        
        verify(paymentPort, never()).processPayment(any());
        verify(persistencePort, never()).saveOrder(any());
    }
    
    private OrderRequest createValidOrderRequest() {
        return OrderRequest.builder()
            .customerId("CUST-123")
            .orderLines(Arrays.asList(
                new OrderLineRequest("PROD-1", 2, BigDecimal.valueOf(10.00))
            ))
            .paymentMethod("CREDIT_CARD")
            .build();
    }
}

// Clean Architecture Use Case Testing
@ExtendWith(MockitoExtension.class)
class CreateUserUseCaseTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private EmailService emailService;
    
    @Mock
    private UserPresenter userPresenter;
    
    @InjectMocks
    private CreateUserUseCaseImpl createUserUseCase;
    
    @Test
    void shouldCreateUserWhenEmailNotExists() {
        // Given
        CreateUserRequest request = new CreateUserRequest("test@example.com", "John Doe");
        
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userPresenter.presentUserCreated(any(User.class)))
            .thenReturn(CreateUserResponse.success(UserId.generate()));
        
        // When
        CreateUserResponse response = createUserUseCase.execute(request);
        
        // Then
        verify(userRepository).save(any(User.class));
        verify(emailService).sendWelcomeEmail("test@example.com", "John Doe");
        verify(userPresenter).presentUserCreated(any(User.class));
    }
    
    @Test
    void shouldFailWhenEmailAlreadyExists() {
        // Given
        CreateUserRequest request = new CreateUserRequest("test@example.com", "John Doe");
        
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);
        when(userPresenter.presentUserAlreadyExists("test@example.com"))
            .thenReturn(CreateUserResponse.failure("User already exists"));
        
        // When
        CreateUserResponse response = createUserUseCase.execute(request);
        
        // Then
        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendWelcomeEmail(any(), any());
        verify(userPresenter).presentUserAlreadyExists("test@example.com");
    }
}

// Integration Testing
@SpringBootTest
@Testcontainers
class OrderIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");
    
    @Autowired
    private OrderApplicationService orderApplicationService;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Test
    @Transactional
    void shouldCreateAndRetrieveOrder() {
        // Given
        CreateOrderCommand command = CreateOrderCommand.builder()
            .customerId(CustomerId.of("CUST-123"))
            .orderLines(Arrays.asList(
                new OrderLineRequest("PROD-1", 2, BigDecimal.valueOf(10.00))
            ))
            .build();
        
        // When
        OrderId orderId = orderApplicationService.createOrder(command);
        
        // Then
        Order savedOrder = orderRepository.findById(orderId);
        assertThat(savedOrder).isNotNull();
        assertThat(savedOrder.getCustomerId().getValue()).isEqualTo("CUST-123");
        assertThat(savedOrder.getOrderLines()).hasSize(1);
    }
}
```

## Practical Exercises

### Exercise 1: Domain-Driven Design Implementation
Build a complete DDD solution that can:
- Model a complex business domain with multiple bounded contexts
- Implement aggregates with proper invariant enforcement
- Create domain services for complex business logic
- Use event storming to discover domain events

### Exercise 2: Hexagonal Architecture Design
Create a hexagonal architecture system that can:
- Separate core business logic from external dependencies
- Implement multiple adapters for the same port
- Test business logic in isolation
- Support different deployment scenarios

### Exercise 3: Clean Architecture Application
Build a clean architecture application that can:
- Implement independent use cases
- Support multiple presentation layers
- Enable independent testing of all layers
- Demonstrate the dependency rule

## Key Performance Metrics

### Architecture Quality Metrics
- **Cyclomatic Complexity**: <10 per method
- **Coupling**: Low coupling between modules
- **Cohesion**: High cohesion within modules
- **Test Coverage**: >90% for domain logic

### Maintainability Metrics
- **Code Duplication**: <5%
- **Technical Debt Ratio**: <20%
- **Documentation Coverage**: >80%
- **Architecture Compliance**: 100%

## Integration with Previous Concepts

### Week 17 Advanced Systems
- **Event Sourcing**: Enhanced with DDD aggregates
- **CQRS**: Advanced patterns with clean separation
- **Distributed Systems**: Applied within bounded contexts
- **High Performance**: Optimized within architectural constraints

### Enterprise Patterns
- **Microservices**: Each bounded context as microservice
- **API Design**: Clean interfaces between layers
- **Security**: Applied at architectural boundaries
- **Monitoring**: Observability across all layers

## Key Takeaways

1. **Domain-Driven Design**: Focus on the business domain and model it accurately
2. **Hexagonal Architecture**: Protect business logic from external concerns
3. **Clean Architecture**: Maintain clear separation of concerns and dependencies
4. **Enterprise Scale**: These patterns enable maintainable systems at scale
5. **Testing Strategy**: Architecture should enable comprehensive testing
6. **Evolution**: Good architecture enables system evolution over time

## Next Steps
Tomorrow we'll explore Advanced Security & Compliance, covering Zero Trust Architecture, SIEM systems, and regulatory frameworks that build upon today's architectural foundations.