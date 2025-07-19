# Day 58: Service Decomposition & Domain Modeling

## Learning Objectives
- Master Domain-Driven Design (DDD) principles for microservices
- Learn service boundary identification techniques
- Understand bounded contexts and aggregate patterns
- Apply decomposition strategies to complex domains
- Design data consistency strategies across services

---

## 1. Domain-Driven Design (DDD) Fundamentals

### 1.1 Core DDD Concepts

Domain-Driven Design provides a systematic approach to understanding and modeling complex business domains, making it ideal for microservices decomposition.

```java
// Domain Model Example - E-commerce Domain
// Value Objects - Immutable objects with no identity
@Value
@Builder
public class Money {
    private final BigDecimal amount;
    private final Currency currency;
    
    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add money with different currencies");
        }
        return Money.builder()
            .amount(this.amount.add(other.amount))
            .currency(this.currency)
            .build();
    }
    
    public Money multiply(BigDecimal multiplier) {
        return Money.builder()
            .amount(this.amount.multiply(multiplier))
            .currency(this.currency)
            .build();
    }
    
    public boolean isGreaterThan(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot compare money with different currencies");
        }
        return this.amount.compareTo(other.amount) > 0;
    }
}

@Value
@Builder
public class Address {
    private final String street;
    private final String city;
    private final String state;
    private final String postalCode;
    private final String country;
    
    public String getFullAddress() {
        return String.format("%s, %s, %s %s, %s", 
            street, city, state, postalCode, country);
    }
    
    public boolean isInCountry(String country) {
        return this.country.equalsIgnoreCase(country);
    }
}

@Value
@Builder
public class Email {
    private final String value;
    
    public static Email of(String email) {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        return Email.builder().value(email.toLowerCase()).build();
    }
    
    public String getDomain() {
        return value.substring(value.indexOf("@") + 1);
    }
}
```

### 1.2 Entities and Aggregates

```java
// Entity - Has identity and lifecycle
@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Embedded
    private CustomerId customerId;
    
    @Embedded
    private Email email;
    
    @Column(nullable = false)
    private String firstName;
    
    @Column(nullable = false)
    private String lastName;
    
    @Embedded
    private Address address;
    
    @Enumerated(EnumType.STRING)
    private CustomerStatus status;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    // Business methods
    public void updateAddress(Address newAddress) {
        this.address = newAddress;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void activate() {
        if (this.status == CustomerStatus.INACTIVE) {
            this.status = CustomerStatus.ACTIVE;
        }
    }
    
    public void deactivate() {
        if (this.status == CustomerStatus.ACTIVE) {
            this.status = CustomerStatus.INACTIVE;
        }
    }
    
    public boolean canPlaceOrder() {
        return this.status == CustomerStatus.ACTIVE;
    }
}

// Aggregate Root - Product with its variants
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Embedded
    private ProductId productId;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String description;
    
    @Embedded
    private Money price;
    
    @Column(nullable = false)
    private String category;
    
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProductVariant> variants = new ArrayList<>();
    
    @Enumerated(EnumType.STRING)
    private ProductStatus status;
    
    // Business methods
    public void addVariant(ProductVariant variant) {
        if (variant == null) {
            throw new IllegalArgumentException("Variant cannot be null");
        }
        variant.setProduct(this);
        this.variants.add(variant);
    }
    
    public void removeVariant(ProductVariant variant) {
        this.variants.remove(variant);
        variant.setProduct(null);
    }
    
    public void updatePrice(Money newPrice) {
        if (newPrice == null || newPrice.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
        this.price = newPrice;
    }
    
    public boolean isAvailable() {
        return this.status == ProductStatus.ACTIVE && 
               this.variants.stream().anyMatch(ProductVariant::isAvailable);
    }
    
    public Money getLowestPrice() {
        return variants.stream()
            .filter(ProductVariant::isAvailable)
            .map(ProductVariant::getPrice)
            .min((p1, p2) -> p1.getAmount().compareTo(p2.getAmount()))
            .orElse(this.price);
    }
}

@Entity
@Table(name = "product_variants")
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String sku;
    
    @Embedded
    private Money price;
    
    @Column(nullable = false)
    private Integer stockQuantity;
    
    @Enumerated(EnumType.STRING)
    private ProductStatus status;
    
    public boolean isAvailable() {
        return this.status == ProductStatus.ACTIVE && this.stockQuantity > 0;
    }
    
    public void reserveStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (this.stockQuantity < quantity) {
            throw new InsufficientStockException("Not enough stock available");
        }
        this.stockQuantity -= quantity;
    }
    
    public void releaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.stockQuantity += quantity;
    }
}
```

### 1.3 Domain Services

```java
// Domain Service - Business logic that doesn't naturally fit in entities
@Service
public class OrderPricingService {
    
    public Money calculateOrderTotal(List<OrderItem> items, Customer customer) {
        Money subtotal = calculateSubtotal(items);
        Money discount = calculateDiscount(subtotal, customer);
        Money tax = calculateTax(subtotal.subtract(discount), customer.getAddress());
        Money shipping = calculateShipping(items, customer.getAddress());
        
        return subtotal.subtract(discount).add(tax).add(shipping);
    }
    
    private Money calculateSubtotal(List<OrderItem> items) {
        return items.stream()
            .map(item -> item.getPrice().multiply(new BigDecimal(item.getQuantity())))
            .reduce(Money::add)
            .orElse(Money.builder().amount(BigDecimal.ZERO).currency(Currency.USD).build());
    }
    
    private Money calculateDiscount(Money subtotal, Customer customer) {
        // Business logic for calculating discounts
        if (customer.getStatus() == CustomerStatus.PREMIUM) {
            return subtotal.multiply(new BigDecimal("0.10")); // 10% discount
        }
        return Money.builder().amount(BigDecimal.ZERO).currency(subtotal.getCurrency()).build();
    }
    
    private Money calculateTax(Money amount, Address address) {
        // Tax calculation based on address
        BigDecimal taxRate = getTaxRate(address.getState());
        return amount.multiply(taxRate);
    }
    
    private Money calculateShipping(List<OrderItem> items, Address address) {
        // Shipping calculation logic
        int totalWeight = items.stream()
            .mapToInt(item -> item.getWeight() * item.getQuantity())
            .sum();
        
        BigDecimal shippingRate = getShippingRate(address.getCountry(), totalWeight);
        return Money.builder()
            .amount(shippingRate)
            .currency(Currency.USD)
            .build();
    }
    
    private BigDecimal getTaxRate(String state) {
        // Tax rate lookup logic
        Map<String, BigDecimal> taxRates = Map.of(
            "CA", new BigDecimal("0.0825"),
            "NY", new BigDecimal("0.08"),
            "TX", new BigDecimal("0.0625")
        );
        return taxRates.getOrDefault(state, BigDecimal.ZERO);
    }
    
    private BigDecimal getShippingRate(String country, int weight) {
        // Shipping rate calculation
        if ("US".equals(country)) {
            return new BigDecimal("5.99").add(new BigDecimal(weight * 0.5));
        }
        return new BigDecimal("19.99").add(new BigDecimal(weight * 1.2));
    }
}
```

---

## 2. Bounded Contexts & Service Boundaries

### 2.1 Identifying Bounded Contexts

Bounded contexts define the boundaries where a particular domain model is applicable.

```java
// User Management Context
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String firstName;
    
    @Column(nullable = false)
    private String lastName;
    
    @Column(nullable = false)
    private String hashedPassword;
    
    @Enumerated(EnumType.STRING)
    private UserStatus status;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    // User management specific methods
    public void changePassword(String newPassword) {
        this.hashedPassword = hashPassword(newPassword);
    }
    
    public void activate() {
        this.status = UserStatus.ACTIVE;
    }
    
    public void deactivate() {
        this.status = UserStatus.INACTIVE;
    }
    
    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }
}

// Order Management Context - Different view of user
@Entity
@Table(name = "order_customers")
public class OrderCustomer {
    @Id
    private Long userId; // References User from User Management Context
    
    @Column(nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String fullName;
    
    @Embedded
    private Address shippingAddress;
    
    @Embedded
    private Address billingAddress;
    
    @Enumerated(EnumType.STRING)
    private CustomerTier tier;
    
    @Column(nullable = false)
    private BigDecimal totalOrderValue;
    
    @Column(nullable = false)
    private Integer orderCount;
    
    // Order context specific methods
    public void addOrder(Money orderValue) {
        this.totalOrderValue = this.totalOrderValue.add(orderValue.getAmount());
        this.orderCount++;
        updateCustomerTier();
    }
    
    private void updateCustomerTier() {
        if (this.totalOrderValue.compareTo(new BigDecimal("10000")) >= 0) {
            this.tier = CustomerTier.PLATINUM;
        } else if (this.totalOrderValue.compareTo(new BigDecimal("5000")) >= 0) {
            this.tier = CustomerTier.GOLD;
        } else if (this.totalOrderValue.compareTo(new BigDecimal("1000")) >= 0) {
            this.tier = CustomerTier.SILVER;
        } else {
            this.tier = CustomerTier.BRONZE;
        }
    }
    
    public BigDecimal getDiscountPercentage() {
        return switch (this.tier) {
            case PLATINUM -> new BigDecimal("0.15");
            case GOLD -> new BigDecimal("0.10");
            case SILVER -> new BigDecimal("0.05");
            default -> BigDecimal.ZERO;
        };
    }
}

// Inventory Management Context - Different view of product
@Entity
@Table(name = "inventory_items")
public class InventoryItem {
    @Id
    private Long productId; // References Product from Catalog Context
    
    @Column(nullable = false)
    private String sku;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private Integer stockQuantity;
    
    @Column(nullable = false)
    private Integer reservedQuantity;
    
    @Column(nullable = false)
    private Integer reorderLevel;
    
    @Column(nullable = false)
    private Integer maxStockLevel;
    
    @Column(nullable = false)
    private String warehouseLocation;
    
    // Inventory specific methods
    public boolean canReserve(int quantity) {
        return getAvailableQuantity() >= quantity;
    }
    
    public void reserve(int quantity) {
        if (!canReserve(quantity)) {
            throw new InsufficientStockException("Cannot reserve " + quantity + " items");
        }
        this.reservedQuantity += quantity;
    }
    
    public void releaseReservation(int quantity) {
        this.reservedQuantity = Math.max(0, this.reservedQuantity - quantity);
    }
    
    public void fulfillReservation(int quantity) {
        if (this.reservedQuantity < quantity) {
            throw new IllegalStateException("Cannot fulfill more than reserved");
        }
        this.stockQuantity -= quantity;
        this.reservedQuantity -= quantity;
    }
    
    public int getAvailableQuantity() {
        return this.stockQuantity - this.reservedQuantity;
    }
    
    public boolean needsRestock() {
        return getAvailableQuantity() <= this.reorderLevel;
    }
}
```

### 2.2 Context Mapping

```java
// Anti-Corruption Layer - Translates between contexts
@Component
public class UserContextAdapter {
    
    @Autowired
    private UserServiceClient userServiceClient;
    
    public OrderCustomer getOrderCustomer(Long userId) {
        // Fetch user from User Management Context
        UserResponse userResponse = userServiceClient.getUser(userId);
        
        // Translate to Order Context model
        return OrderCustomer.builder()
            .userId(userResponse.getId())
            .email(userResponse.getEmail())
            .fullName(userResponse.getFirstName() + " " + userResponse.getLastName())
            .shippingAddress(translateAddress(userResponse.getAddress()))
            .billingAddress(translateAddress(userResponse.getBillingAddress()))
            .tier(CustomerTier.BRONZE) // Default tier
            .totalOrderValue(BigDecimal.ZERO)
            .orderCount(0)
            .build();
    }
    
    private Address translateAddress(UserAddressResponse userAddress) {
        if (userAddress == null) {
            return null;
        }
        
        return Address.builder()
            .street(userAddress.getStreet())
            .city(userAddress.getCity())
            .state(userAddress.getState())
            .postalCode(userAddress.getPostalCode())
            .country(userAddress.getCountry())
            .build();
    }
}

// Shared Kernel - Common domain concepts
@Entity
@Table(name = "domain_events")
public class DomainEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String eventType;
    
    @Column(nullable = false)
    private String aggregateId;
    
    @Column(nullable = false)
    private String aggregateType;
    
    @Column(columnDefinition = "jsonb")
    private String eventData;
    
    @CreationTimestamp
    private LocalDateTime occurredAt;
    
    @Column(nullable = false)
    private String contextName;
    
    // Static factory methods
    public static DomainEvent userCreated(Long userId, String userData) {
        return DomainEvent.builder()
            .eventType("UserCreated")
            .aggregateId(userId.toString())
            .aggregateType("User")
            .eventData(userData)
            .contextName("UserManagement")
            .build();
    }
    
    public static DomainEvent orderPlaced(Long orderId, String orderData) {
        return DomainEvent.builder()
            .eventType("OrderPlaced")
            .aggregateId(orderId.toString())
            .aggregateType("Order")
            .eventData(orderData)
            .contextName("OrderManagement")
            .build();
    }
}
```

---

## 3. Service Decomposition Strategies

### 3.1 Decompose by Business Capability

```java
// Customer Management Service
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {
    
    @Autowired
    private CustomerService customerService;
    
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        Customer customer = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(CustomerResponse.from(customer));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable Long id) {
        Customer customer = customerService.getCustomer(id);
        return ResponseEntity.ok(CustomerResponse.from(customer));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCustomerRequest request) {
        Customer customer = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(CustomerResponse.from(customer));
    }
    
    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activateCustomer(@PathVariable Long id) {
        customerService.activateCustomer(id);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateCustomer(@PathVariable Long id) {
        customerService.deactivateCustomer(id);
        return ResponseEntity.ok().build();
    }
}

@Service
@Transactional
public class CustomerService {
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private DomainEventPublisher eventPublisher;
    
    public Customer createCustomer(CreateCustomerRequest request) {
        // Business validation
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new CustomerAlreadyExistsException("Customer with email already exists");
        }
        
        Customer customer = Customer.builder()
            .customerId(CustomerId.generate())
            .email(Email.of(request.getEmail()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .address(request.getAddress())
            .status(CustomerStatus.ACTIVE)
            .build();
        
        Customer savedCustomer = customerRepository.save(customer);
        
        // Publish domain event
        eventPublisher.publishEvent(DomainEvent.customerCreated(
            savedCustomer.getId(), 
            toJson(savedCustomer)
        ));
        
        return savedCustomer;
    }
    
    public Customer updateCustomer(Long id, UpdateCustomerRequest request) {
        Customer customer = getCustomer(id);
        
        customer.updateAddress(request.getAddress());
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        
        Customer updatedCustomer = customerRepository.save(customer);
        
        // Publish domain event
        eventPublisher.publishEvent(DomainEvent.customerUpdated(
            updatedCustomer.getId(),
            toJson(updatedCustomer)
        ));
        
        return updatedCustomer;
    }
    
    public void activateCustomer(Long id) {
        Customer customer = getCustomer(id);
        customer.activate();
        customerRepository.save(customer);
        
        eventPublisher.publishEvent(DomainEvent.customerActivated(id));
    }
    
    public void deactivateCustomer(Long id) {
        Customer customer = getCustomer(id);
        customer.deactivate();
        customerRepository.save(customer);
        
        eventPublisher.publishEvent(DomainEvent.customerDeactivated(id));
    }
    
    public Customer getCustomer(Long id) {
        return customerRepository.findById(id)
            .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + id));
    }
}
```

### 3.2 Decompose by Data

```java
// Product Catalog Service - Handles product information
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    
    @Autowired
    private ProductService productService;
    
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        Product product = productService.getProduct(id);
        return ResponseEntity.ok(ProductResponse.from(product));
    }
    
    @GetMapping
    public ResponseEntity<PagedResponse<ProductResponse>> searchProducts(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "") String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<Product> products = productService.searchProducts(query, category, page, size);
        PagedResponse<ProductResponse> response = PagedResponse.from(
            products, ProductResponse::from);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        Product product = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.from(product));
    }
}

// Inventory Service - Handles stock management
@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {
    
    @Autowired
    private InventoryService inventoryService;
    
    @PostMapping("/check")
    public ResponseEntity<InventoryCheckResponse> checkInventory(
            @RequestBody InventoryCheckRequest request) {
        
        Map<Long, Integer> availability = inventoryService.checkAvailability(
            request.getProductIds(), request.getQuantities());
        
        InventoryCheckResponse response = InventoryCheckResponse.builder()
            .availability(availability)
            .allAvailable(availability.values().stream().allMatch(qty -> qty > 0))
            .build();
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/reserve")
    public ResponseEntity<ReservationResponse> reserveInventory(
            @RequestBody ReservationRequest request) {
        
        String reservationId = inventoryService.reserveInventory(
            request.getProductIds(), 
            request.getQuantities(),
            request.getCustomerId());
        
        ReservationResponse response = ReservationResponse.builder()
            .reservationId(reservationId)
            .expiresAt(LocalDateTime.now().plusMinutes(15))
            .build();
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/fulfill/{reservationId}")
    public ResponseEntity<Void> fulfillReservation(@PathVariable String reservationId) {
        inventoryService.fulfillReservation(reservationId);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/cancel/{reservationId}")
    public ResponseEntity<Void> cancelReservation(@PathVariable String reservationId) {
        inventoryService.cancelReservation(reservationId);
        return ResponseEntity.ok().build();
    }
}

@Service
@Transactional
public class InventoryService {
    
    @Autowired
    private InventoryRepository inventoryRepository;
    
    @Autowired
    private ReservationRepository reservationRepository;
    
    @Autowired
    private DomainEventPublisher eventPublisher;
    
    public Map<Long, Integer> checkAvailability(List<Long> productIds, List<Integer> quantities) {
        Map<Long, Integer> availability = new HashMap<>();
        
        for (int i = 0; i < productIds.size(); i++) {
            Long productId = productIds.get(i);
            Integer requestedQuantity = quantities.get(i);
            
            InventoryItem item = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + productId));
            
            int availableQuantity = item.getAvailableQuantity();
            availability.put(productId, Math.min(availableQuantity, requestedQuantity));
        }
        
        return availability;
    }
    
    public String reserveInventory(List<Long> productIds, List<Integer> quantities, Long customerId) {
        String reservationId = UUID.randomUUID().toString();
        
        try {
            for (int i = 0; i < productIds.size(); i++) {
                Long productId = productIds.get(i);
                Integer quantity = quantities.get(i);
                
                InventoryItem item = inventoryRepository.findByProductId(productId)
                    .orElseThrow(() -> new ProductNotFoundException("Product not found: " + productId));
                
                if (!item.canReserve(quantity)) {
                    throw new InsufficientStockException("Insufficient stock for product: " + productId);
                }
                
                item.reserve(quantity);
                inventoryRepository.save(item);
                
                // Create reservation record
                Reservation reservation = Reservation.builder()
                    .reservationId(reservationId)
                    .productId(productId)
                    .quantity(quantity)
                    .customerId(customerId)
                    .status(ReservationStatus.ACTIVE)
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .build();
                
                reservationRepository.save(reservation);
            }
            
            // Publish domain event
            eventPublisher.publishEvent(DomainEvent.inventoryReserved(
                reservationId, customerId, productIds, quantities));
            
            return reservationId;
            
        } catch (Exception e) {
            // Rollback reservations on failure
            rollbackReservations(reservationId);
            throw e;
        }
    }
    
    private void rollbackReservations(String reservationId) {
        List<Reservation> reservations = reservationRepository.findByReservationId(reservationId);
        
        for (Reservation reservation : reservations) {
            InventoryItem item = inventoryRepository.findByProductId(reservation.getProductId())
                .orElse(null);
            
            if (item != null) {
                item.releaseReservation(reservation.getQuantity());
                inventoryRepository.save(item);
            }
            
            reservation.setStatus(ReservationStatus.CANCELLED);
            reservationRepository.save(reservation);
        }
    }
}
```

### 3.3 Decompose by Use Case

```java
// Order Processing Service - Handles order creation workflow
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    
    @Autowired
    private OrderProcessingService orderProcessingService;
    
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Order order = orderProcessingService.processOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(order));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        Order order = orderProcessingService.getOrder(id);
        return ResponseEntity.ok(OrderResponse.from(order));
    }
    
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long id) {
        orderProcessingService.cancelOrder(id);
        return ResponseEntity.ok().build();
    }
}

@Service
@Transactional
public class OrderProcessingService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private CustomerServiceClient customerServiceClient;
    
    @Autowired
    private InventoryServiceClient inventoryServiceClient;
    
    @Autowired
    private PaymentServiceClient paymentServiceClient;
    
    @Autowired
    private OrderPricingService orderPricingService;
    
    @Autowired
    private DomainEventPublisher eventPublisher;
    
    public Order processOrder(CreateOrderRequest request) {
        String transactionId = UUID.randomUUID().toString();
        
        try {
            // Step 1: Validate customer
            CustomerResponse customer = customerServiceClient.getCustomer(request.getCustomerId());
            if (!customer.isActive()) {
                throw new InactiveCustomerException("Customer is not active");
            }
            
            // Step 2: Reserve inventory
            ReservationRequest reservationRequest = ReservationRequest.builder()
                .productIds(request.getItems().stream().map(OrderItemRequest::getProductId).collect(Collectors.toList()))
                .quantities(request.getItems().stream().map(OrderItemRequest::getQuantity).collect(Collectors.toList()))
                .customerId(request.getCustomerId())
                .build();
            
            ReservationResponse reservation = inventoryServiceClient.reserveInventory(reservationRequest);
            
            // Step 3: Calculate pricing
            List<OrderItem> orderItems = createOrderItems(request.getItems());
            Money totalAmount = orderPricingService.calculateOrderTotal(orderItems, customer);
            
            // Step 4: Create order
            Order order = Order.builder()
                .customerId(request.getCustomerId())
                .items(orderItems)
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING)
                .reservationId(reservation.getReservationId())
                .transactionId(transactionId)
                .build();
            
            Order savedOrder = orderRepository.save(order);
            
            // Step 5: Process payment asynchronously
            PaymentRequest paymentRequest = PaymentRequest.builder()
                .orderId(savedOrder.getId())
                .amount(totalAmount)
                .paymentMethod(request.getPaymentMethod())
                .transactionId(transactionId)
                .build();
            
            paymentServiceClient.processPaymentAsync(paymentRequest);
            
            // Publish domain event
            eventPublisher.publishEvent(DomainEvent.orderCreated(
                savedOrder.getId(), 
                toJson(savedOrder)
            ));
            
            return savedOrder;
            
        } catch (Exception e) {
            // Handle failure - cancel reservations, etc.
            handleOrderCreationFailure(transactionId, e);
            throw e;
        }
    }
    
    private List<OrderItem> createOrderItems(List<OrderItemRequest> itemRequests) {
        return itemRequests.stream()
            .map(this::createOrderItem)
            .collect(Collectors.toList());
    }
    
    private OrderItem createOrderItem(OrderItemRequest request) {
        // This would typically involve calling the product service
        // to get current product information
        return OrderItem.builder()
            .productId(request.getProductId())
            .quantity(request.getQuantity())
            .unitPrice(request.getUnitPrice())
            .build();
    }
    
    private void handleOrderCreationFailure(String transactionId, Exception e) {
        logger.error("Order creation failed for transaction {}: {}", transactionId, e.getMessage());
        
        // Cancel any reservations
        try {
            inventoryServiceClient.cancelReservationByTransactionId(transactionId);
        } catch (Exception ex) {
            logger.error("Failed to cancel reservations for transaction {}: {}", transactionId, ex.getMessage());
        }
        
        // Publish failure event
        eventPublisher.publishEvent(DomainEvent.orderCreationFailed(
            transactionId, e.getMessage()));
    }
}
```

---

## 4. Data Consistency Strategies

### 4.1 Eventual Consistency with Saga Pattern

```java
// Order Saga Orchestrator
@Component
public class OrderSagaOrchestrator {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private InventoryServiceClient inventoryServiceClient;
    
    @Autowired
    private PaymentServiceClient paymentServiceClient;
    
    @Autowired
    private ShippingServiceClient shippingServiceClient;
    
    @Autowired
    private DomainEventPublisher eventPublisher;
    
    @EventListener
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        Order order = orderRepository.findById(event.getOrderId())
            .orElseThrow(() -> new OrderNotFoundException("Order not found"));
        
        // Start saga
        SagaTransaction saga = SagaTransaction.builder()
            .orderId(order.getId())
            .transactionId(order.getTransactionId())
            .status(SagaStatus.STARTED)
            .currentStep(SagaStep.INVENTORY_RESERVATION)
            .build();
        
        // Execute first step
        executeInventoryReservation(saga, order);
    }
    
    private void executeInventoryReservation(SagaTransaction saga, Order order) {
        try {
            ReservationRequest request = createReservationRequest(order);
            ReservationResponse response = inventoryServiceClient.reserveInventory(request);
            
            // Update saga
            saga.setCurrentStep(SagaStep.PAYMENT_PROCESSING);
            saga.setInventoryReservationId(response.getReservationId());
            
            // Execute next step
            executePaymentProcessing(saga, order);
            
        } catch (Exception e) {
            handleSagaFailure(saga, SagaStep.INVENTORY_RESERVATION, e);
        }
    }
    
    private void executePaymentProcessing(SagaTransaction saga, Order order) {
        try {
            PaymentRequest request = createPaymentRequest(order);
            PaymentResponse response = paymentServiceClient.processPayment(request);
            
            if (response.isSuccess()) {
                // Update saga
                saga.setCurrentStep(SagaStep.SHIPPING_ARRANGEMENT);
                saga.setPaymentId(response.getPaymentId());
                
                // Execute next step
                executeShippingArrangement(saga, order);
            } else {
                throw new PaymentFailedException("Payment failed: " + response.getErrorMessage());
            }
            
        } catch (Exception e) {
            handleSagaFailure(saga, SagaStep.PAYMENT_PROCESSING, e);
        }
    }
    
    private void executeShippingArrangement(SagaTransaction saga, Order order) {
        try {
            ShippingRequest request = createShippingRequest(order);
            ShippingResponse response = shippingServiceClient.arrangeShipping(request);
            
            // Update saga
            saga.setCurrentStep(SagaStep.COMPLETED);
            saga.setStatus(SagaStatus.COMPLETED);
            saga.setShippingId(response.getShippingId());
            
            // Update order status
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
            
            // Publish success event
            eventPublisher.publishEvent(DomainEvent.orderConfirmed(
                order.getId(), order.getCustomerId()));
            
        } catch (Exception e) {
            handleSagaFailure(saga, SagaStep.SHIPPING_ARRANGEMENT, e);
        }
    }
    
    private void handleSagaFailure(SagaTransaction saga, SagaStep failedStep, Exception e) {
        logger.error("Saga failed at step {} for order {}: {}", 
            failedStep, saga.getOrderId(), e.getMessage());
        
        saga.setStatus(SagaStatus.FAILED);
        saga.setFailureReason(e.getMessage());
        
        // Execute compensating actions
        executeCompensatingActions(saga, failedStep);
        
        // Update order status
        Order order = orderRepository.findById(saga.getOrderId()).orElse(null);
        if (order != null) {
            order.setStatus(OrderStatus.FAILED);
            order.setFailureReason(e.getMessage());
            orderRepository.save(order);
        }
        
        // Publish failure event
        eventPublisher.publishEvent(DomainEvent.orderFailed(
            saga.getOrderId(), e.getMessage()));
    }
    
    private void executeCompensatingActions(SagaTransaction saga, SagaStep failedStep) {
        // Compensate in reverse order
        switch (failedStep) {
            case SHIPPING_ARRANGEMENT:
                compensatePayment(saga);
                // Fall through
            case PAYMENT_PROCESSING:
                compensateInventoryReservation(saga);
                break;
            case INVENTORY_RESERVATION:
                // Nothing to compensate
                break;
        }
    }
    
    private void compensatePayment(SagaTransaction saga) {
        if (saga.getPaymentId() != null) {
            try {
                paymentServiceClient.refundPayment(saga.getPaymentId());
            } catch (Exception e) {
                logger.error("Failed to refund payment {} for order {}: {}", 
                    saga.getPaymentId(), saga.getOrderId(), e.getMessage());
            }
        }
    }
    
    private void compensateInventoryReservation(SagaTransaction saga) {
        if (saga.getInventoryReservationId() != null) {
            try {
                inventoryServiceClient.cancelReservation(saga.getInventoryReservationId());
            } catch (Exception e) {
                logger.error("Failed to cancel reservation {} for order {}: {}", 
                    saga.getInventoryReservationId(), saga.getOrderId(), e.getMessage());
            }
        }
    }
}

// Saga Transaction Entity
@Entity
@Table(name = "saga_transactions")
public class SagaTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long orderId;
    
    @Column(nullable = false)
    private String transactionId;
    
    @Enumerated(EnumType.STRING)
    private SagaStatus status;
    
    @Enumerated(EnumType.STRING)
    private SagaStep currentStep;
    
    private String inventoryReservationId;
    private String paymentId;
    private String shippingId;
    
    private String failureReason;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    // Getters and setters
}

public enum SagaStatus {
    STARTED, COMPLETED, FAILED, COMPENSATING
}

public enum SagaStep {
    INVENTORY_RESERVATION, PAYMENT_PROCESSING, SHIPPING_ARRANGEMENT, COMPLETED
}
```

### 4.2 Event Sourcing for Audit Trail

```java
// Event Store
@Entity
@Table(name = "event_store")
public class EventStore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String aggregateId;
    
    @Column(nullable = false)
    private String aggregateType;
    
    @Column(nullable = false)
    private String eventType;
    
    @Column(columnDefinition = "jsonb")
    private String eventData;
    
    @Column(nullable = false)
    private Long version;
    
    @CreationTimestamp
    private LocalDateTime timestamp;
    
    // Getters and setters
}

// Event Sourced Aggregate
@Entity
@Table(name = "order_aggregates")
public class OrderAggregate {
    @Id
    private String aggregateId;
    
    @Column(nullable = false)
    private Long version;
    
    @Transient
    private List<DomainEvent> uncommittedEvents = new ArrayList<>();
    
    @Transient
    private OrderState currentState;
    
    public void applyEvent(DomainEvent event) {
        switch (event.getEventType()) {
            case "OrderCreated":
                apply((OrderCreatedEvent) event);
                break;
            case "OrderConfirmed":
                apply((OrderConfirmedEvent) event);
                break;
            case "OrderCancelled":
                apply((OrderCancelledEvent) event);
                break;
            case "OrderShipped":
                apply((OrderShippedEvent) event);
                break;
        }
        
        this.version++;
        this.uncommittedEvents.add(event);
    }
    
    private void apply(OrderCreatedEvent event) {
        this.currentState = OrderState.builder()
            .orderId(event.getOrderId())
            .customerId(event.getCustomerId())
            .totalAmount(event.getTotalAmount())
            .status(OrderStatus.PENDING)
            .items(event.getItems())
            .build();
    }
    
    private void apply(OrderConfirmedEvent event) {
        this.currentState = this.currentState.toBuilder()
            .status(OrderStatus.CONFIRMED)
            .confirmedAt(event.getConfirmedAt())
            .build();
    }
    
    private void apply(OrderCancelledEvent event) {
        this.currentState = this.currentState.toBuilder()
            .status(OrderStatus.CANCELLED)
            .cancelledAt(event.getCancelledAt())
            .cancellationReason(event.getReason())
            .build();
    }
    
    private void apply(OrderShippedEvent event) {
        this.currentState = this.currentState.toBuilder()
            .status(OrderStatus.SHIPPED)
            .shippedAt(event.getShippedAt())
            .trackingNumber(event.getTrackingNumber())
            .build();
    }
    
    public List<DomainEvent> getUncommittedEvents() {
        return new ArrayList<>(uncommittedEvents);
    }
    
    public void markEventsAsCommitted() {
        this.uncommittedEvents.clear();
    }
}

// Event Sourced Repository
@Repository
public class EventSourcedOrderRepository {
    
    @Autowired
    private EventStoreRepository eventStoreRepository;
    
    @Autowired
    private OrderAggregateRepository orderAggregateRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public void save(OrderAggregate aggregate) {
        // Save events to event store
        for (DomainEvent event : aggregate.getUncommittedEvents()) {
            EventStore eventStore = new EventStore();
            eventStore.setAggregateId(aggregate.getAggregateId());
            eventStore.setAggregateType("Order");
            eventStore.setEventType(event.getEventType());
            eventStore.setEventData(serializeEvent(event));
            eventStore.setVersion(aggregate.getVersion());
            
            eventStoreRepository.save(eventStore);
        }
        
        // Save aggregate snapshot
        orderAggregateRepository.save(aggregate);
        
        // Mark events as committed
        aggregate.markEventsAsCommitted();
    }
    
    public OrderAggregate findByAggregateId(String aggregateId) {
        // Load aggregate snapshot
        OrderAggregate aggregate = orderAggregateRepository.findByAggregateId(aggregateId)
            .orElse(new OrderAggregate());
        
        // Load events since last snapshot
        List<EventStore> events = eventStoreRepository.findByAggregateIdAndVersionGreaterThan(
            aggregateId, aggregate.getVersion());
        
        // Replay events
        for (EventStore eventStore : events) {
            DomainEvent event = deserializeEvent(eventStore.getEventData(), eventStore.getEventType());
            aggregate.applyEvent(event);
        }
        
        return aggregate;
    }
    
    private String serializeEvent(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new EventSerializationException("Failed to serialize event", e);
        }
    }
    
    private DomainEvent deserializeEvent(String eventData, String eventType) {
        try {
            Class<?> eventClass = Class.forName("com.example.events." + eventType);
            return (DomainEvent) objectMapper.readValue(eventData, eventClass);
        } catch (Exception e) {
            throw new EventDeserializationException("Failed to deserialize event", e);
        }
    }
}
```

---

## 5. Key Takeaways

### Domain-Driven Design Benefits:
✅ **Clear Business Boundaries**: Well-defined service boundaries based on business domains  
✅ **Ubiquitous Language**: Shared vocabulary between business and technical teams  
✅ **Rich Domain Models**: Business logic encapsulated in domain objects  
✅ **Bounded Contexts**: Clear context boundaries prevent model confusion  

### Service Decomposition Strategies:
1. **By Business Capability**: Services aligned with business functions
2. **By Data**: Services own their data and business logic
3. **By Use Case**: Services handle specific business workflows
4. **By Team Structure**: Services aligned with organizational structure

### Data Consistency Patterns:
- **Eventual Consistency**: Accept temporary inconsistency for better availability
- **Saga Pattern**: Manage distributed transactions with compensating actions
- **Event Sourcing**: Maintain complete audit trail of all changes
- **CQRS**: Separate read and write models for better scalability

---

## 6. Next Steps

Tomorrow we'll explore **Inter-Service Communication** patterns, learning:
- Synchronous vs asynchronous communication
- REST API design for microservices
- Message-based communication patterns
- Circuit breaker and retry mechanisms

**Practice Assignment**: Design service boundaries for your domain using DDD principles and identify data consistency requirements.

---

*"Good service boundaries are discovered, not invented. They emerge from understanding the business domain and its natural divisions."*

*"The hardest part of microservices is not the technology - it's getting the boundaries right."*