# Day 20: Spring Data JPA (July 29, 2025)

**Date:** July 29, 2025  
**Duration:** 2.5 hours  
**Focus:** Master Spring Data JPA for database integration, entity mapping, relationships, and repository patterns

---

## 🎯 Today's Learning Goals

By the end of this session, you'll:
- ✅ Understand JPA concepts and entity mapping
- ✅ Master Spring Data repositories and query methods
- ✅ Implement entity relationships (One-to-One, One-to-Many, Many-to-Many)
- ✅ Use JPQL and native queries for complex operations
- ✅ Handle transactions and optimistic locking
- ✅ Build a complete e-commerce system with Spring Data JPA

---

## 🗄️ Part 1: JPA Fundamentals and Entity Mapping (45 minutes)

### **Understanding JPA and Spring Data JPA**

**JPA (Java Persistence API)** is a specification for managing relational data in Java applications, while **Spring Data JPA** provides a repository abstraction on top of JPA to simplify data access.

**Key Concepts:**
- **Entity**: Java object mapped to database table
- **Entity Manager**: Interface for database operations
- **Repository**: Spring Data abstraction for data access
- **Transaction**: Unit of work with ACID properties

### **Entity Mapping Fundamentals**

Create `SpringDataJpaFundamentalsDemo.java`:

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Spring Data JPA fundamentals demonstration
 * Shows entity mapping, repositories, and basic CRUD operations
 */
@SpringBootApplication
public class SpringDataJpaFundamentalsDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Spring Data JPA Fundamentals Demo ===\n");
        SpringApplication.run(SpringDataJpaFundamentalsDemo.class, args);
    }
    
    @Bean
    public CommandLineRunner demo(CustomerRepository customerRepository,
                                 ProductRepository productRepository,
                                 OrderRepository orderRepository) {
        return (args) -> {
            System.out.println("🚀 Spring Data JPA Demo Started!\n");
            
            // Demonstrate entity mapping
            demonstrateEntityMapping(customerRepository);
            
            // Demonstrate repository methods
            demonstrateRepositoryMethods(productRepository);
            
            // Demonstrate query methods
            demonstrateQueryMethods(customerRepository, productRepository);
            
            // Demonstrate custom queries
            demonstrateCustomQueries(productRepository);
            
            System.out.println("\n✅ Demo completed successfully!");
        };
    }
    
    @Transactional
    private void demonstrateEntityMapping(CustomerRepository customerRepository) {
        System.out.println("1. Entity Mapping Demonstration:\n");
        
        // Create and save customer
        Customer customer = new Customer();
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("john.doe@example.com");
        customer.setPhone("+1-555-0123");
        
        // Address embedded
        Address address = new Address();
        address.setStreet("123 Main St");
        address.setCity("Springfield");
        address.setState("IL");
        address.setZipCode("62701");
        address.setCountry("USA");
        customer.setAddress(address);
        
        Customer savedCustomer = customerRepository.save(customer);
        System.out.println("✅ Customer saved with ID: " + savedCustomer.getId());
        System.out.println("   Created at: " + savedCustomer.getCreatedAt());
        System.out.println("   Full name: " + savedCustomer.getFullName());
        System.out.println("   Address: " + savedCustomer.getAddress().getFullAddress());
    }
    
    @Transactional
    private void demonstrateRepositoryMethods(ProductRepository productRepository) {
        System.out.println("\n2. Repository Methods Demonstration:\n");
        
        // Create multiple products
        List<Product> products = Arrays.asList(
            createProduct("Laptop", "High-performance laptop", new BigDecimal("999.99"), "Electronics", 50),
            createProduct("Mouse", "Wireless mouse", new BigDecimal("29.99"), "Electronics", 200),
            createProduct("Keyboard", "Mechanical keyboard", new BigDecimal("79.99"), "Electronics", 150),
            createProduct("Monitor", "4K monitor", new BigDecimal("399.99"), "Electronics", 75),
            createProduct("Desk", "Standing desk", new BigDecimal("599.99"), "Furniture", 30)
        );
        
        // Save all products
        List<Product> savedProducts = productRepository.saveAll(products);
        System.out.println("✅ Saved " + savedProducts.size() + " products");
        
        // Count products
        long count = productRepository.count();
        System.out.println("📊 Total products: " + count);
        
        // Find by ID
        Optional<Product> product = productRepository.findById(savedProducts.get(0).getId());
        product.ifPresent(p -> System.out.println("🔍 Found product: " + p.getName()));
        
        // Check existence
        boolean exists = productRepository.existsById(savedProducts.get(0).getId());
        System.out.println("❓ Product exists: " + exists);
        
        // Delete product
        productRepository.deleteById(savedProducts.get(4).getId());
        System.out.println("🗑️ Deleted one product");
        System.out.println("📊 Remaining products: " + productRepository.count());
    }
    
    @Transactional
    private void demonstrateQueryMethods(CustomerRepository customerRepository, 
                                       ProductRepository productRepository) {
        System.out.println("\n3. Query Methods Demonstration:\n");
        
        // Create test data
        customerRepository.saveAll(Arrays.asList(
            createCustomer("Jane", "Smith", "jane@example.com"),
            createCustomer("Bob", "Johnson", "bob@example.com"),
            createCustomer("Alice", "Brown", "alice@example.com")
        ));
        
        // Find by email
        Customer customer = customerRepository.findByEmail("jane@example.com");
        if (customer != null) {
            System.out.println("📧 Found customer by email: " + customer.getFullName());
        }
        
        // Find by last name
        List<Customer> smiths = customerRepository.findByLastName("Smith");
        System.out.println("👥 Customers with last name 'Smith': " + smiths.size());
        
        // Find products by category
        List<Product> electronics = productRepository.findByCategory("Electronics");
        System.out.println("📱 Electronics products: " + electronics.size());
        
        // Find products by price range
        List<Product> affordableProducts = productRepository.findByPriceBetween(
            new BigDecimal("20.00"), new BigDecimal("100.00"));
        System.out.println("💰 Products between $20-$100: " + affordableProducts.size());
        
        // Find products with low stock
        List<Product> lowStock = productRepository.findByStockQuantityLessThan(50);
        System.out.println("⚠️ Products with low stock (<50): " + lowStock.size());
    }
    
    @Transactional
    private void demonstrateCustomQueries(ProductRepository productRepository) {
        System.out.println("\n4. Custom Queries Demonstration:\n");
        
        // JPQL query
        List<Product> expensiveProducts = productRepository.findExpensiveProducts(new BigDecimal("100.00"));
        System.out.println("💎 Expensive products (>$100): " + expensiveProducts.size());
        expensiveProducts.forEach(p -> 
            System.out.println("   - " + p.getName() + ": $" + p.getPrice()));
        
        // Native query
        List<Product> topProducts = productRepository.findTopSellingProducts(3);
        System.out.println("\n🏆 Top 3 products by stock:");
        topProducts.forEach(p -> 
            System.out.println("   - " + p.getName() + " (stock: " + p.getStockQuantity() + ")"));
        
        // Update query
        int updated = productRepository.updateProductPrices("Electronics", new BigDecimal("0.9"));
        System.out.println("\n💸 Applied 10% discount to " + updated + " electronics products");
        
        // Category statistics
        List<CategoryStats> stats = productRepository.getCategoryStatistics();
        System.out.println("\n📊 Category Statistics:");
        stats.forEach(s -> 
            System.out.println("   - " + s.getCategory() + ": " + s.getProductCount() + 
                             " products, avg price: $" + s.getAveragePrice()));
    }
    
    private Product createProduct(String name, String description, BigDecimal price, 
                                String category, int stock) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setCategory(category);
        product.setStockQuantity(stock);
        return product;
    }
    
    private Customer createCustomer(String firstName, String lastName, String email) {
        Customer customer = new Customer();
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setEmail(email);
        return customer;
    }
}

// Base entity with common fields
@MappedSuperclass
abstract class BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    
    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;
    
    @Version
    private Long version;
    
    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        updatedAt = new Date();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}

// Customer entity
@Entity
@Table(name = "customers", 
       indexes = {
           @Index(name = "idx_customer_email", columnList = "email"),
           @Index(name = "idx_customer_last_name", columnList = "last_name")
       })
public class Customer extends BaseEntity {
    
    @NotBlank(message = "First name is required")
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Column(nullable = false, unique = true, length = 100)
    private String email;
    
    @Column(length = 20)
    private String phone;
    
    @Embedded
    private Address address;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", length = 20)
    private CustomerType customerType = CustomerType.REGULAR;
    
    @Column(name = "loyalty_points")
    private Integer loyaltyPoints = 0;
    
    // Transient field (not persisted)
    @Transient
    private String fullName;
    
    // Getters and setters
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public Address getAddress() { return address; }
    public void setAddress(Address address) { this.address = address; }
    
    public CustomerType getCustomerType() { return customerType; }
    public void setCustomerType(CustomerType customerType) { this.customerType = customerType; }
    
    public Integer getLoyaltyPoints() { return loyaltyPoints; }
    public void setLoyaltyPoints(Integer loyaltyPoints) { this.loyaltyPoints = loyaltyPoints; }
    
    // Helper method
    public String getFullName() {
        return firstName + " " + lastName;
    }
}

// Embeddable address
@Embeddable
class Address {
    
    @Column(length = 100)
    private String street;
    
    @Column(length = 50)
    private String city;
    
    @Column(length = 2)
    private String state;
    
    @Column(name = "zip_code", length = 10)
    private String zipCode;
    
    @Column(length = 50)
    private String country;
    
    // Getters and setters
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    
    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }
    
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    
    public String getFullAddress() {
        return street + ", " + city + ", " + state + " " + zipCode + ", " + country;
    }
}

// Product entity
@Entity
@Table(name = "products")
@NamedQueries({
    @NamedQuery(name = "Product.findByCategory",
                query = "SELECT p FROM Product p WHERE p.category = :category"),
    @NamedQuery(name = "Product.findActive",
                query = "SELECT p FROM Product p WHERE p.active = true")
})
public class Product extends BaseEntity {
    
    @NotBlank(message = "Product name is required")
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.00", inclusive = false)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @Column(length = 50)
    private String category;
    
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;
    
    @Column(nullable = false)
    private Boolean active = true;
    
    @Column(name = "image_url")
    private String imageUrl;
    
    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
    
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}

// Order entity (placeholder for relationships)
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {
    
    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrderStatus status = OrderStatus.PENDING;
    
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    // Getters and setters
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
}

// Enums
enum CustomerType {
    REGULAR, PREMIUM, VIP
}

enum OrderStatus {
    PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED
}

// Repositories
@Repository
interface CustomerRepository extends JpaRepository<Customer, Long> {
    
    Customer findByEmail(String email);
    
    List<Customer> findByLastName(String lastName);
    
    List<Customer> findByCustomerType(CustomerType type);
    
    List<Customer> findByLoyaltyPointsGreaterThan(Integer points);
    
    @Query("SELECT c FROM Customer c WHERE c.address.city = :city")
    List<Customer> findByCity(@Param("city") String city);
}

@Repository
interface ProductRepository extends JpaRepository<Product, Long> {
    
    List<Product> findByCategory(String category);
    
    List<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
    
    List<Product> findByStockQuantityLessThan(Integer quantity);
    
    List<Product> findByActiveTrue();
    
    @Query("SELECT p FROM Product p WHERE p.price > :price ORDER BY p.price DESC")
    List<Product> findExpensiveProducts(@Param("price") BigDecimal price);
    
    @Query(value = "SELECT * FROM products ORDER BY stock_quantity DESC LIMIT :limit", 
           nativeQuery = true)
    List<Product> findTopSellingProducts(@Param("limit") int limit);
    
    @Modifying
    @Query("UPDATE Product p SET p.price = p.price * :multiplier WHERE p.category = :category")
    int updateProductPrices(@Param("category") String category, 
                           @Param("multiplier") BigDecimal multiplier);
    
    @Query("SELECT new com.example.CategoryStats(p.category, COUNT(p), AVG(p.price)) " +
           "FROM Product p GROUP BY p.category")
    List<CategoryStats> getCategoryStatistics();
}

@Repository
interface OrderRepository extends JpaRepository<Order, Long> {
    
    Order findByOrderNumber(String orderNumber);
    
    List<Order> findByStatus(OrderStatus status);
}

// DTO for statistics
class CategoryStats {
    private String category;
    private Long productCount;
    private Double averagePrice;
    
    public CategoryStats(String category, Long productCount, Double averagePrice) {
        this.category = category;
        this.productCount = productCount;
        this.averagePrice = averagePrice;
    }
    
    // Getters
    public String getCategory() { return category; }
    public Long getProductCount() { return productCount; }
    public Double getAveragePrice() { return averagePrice; }
}
```

---

## 🔗 Part 2: Entity Relationships (45 minutes)

### **JPA Relationship Mapping**

JPA supports all standard database relationships with various cascade and fetch strategies.

### **Relationship Mapping Demo**

Create `JpaRelationshipsDemo.java`:

```java
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * JPA relationship mapping demonstration
 * Shows One-to-One, One-to-Many, Many-to-Many relationships
 */
@SpringBootApplication
public class JpaRelationshipsDemo {
    
    public static void main(String[] args) {
        System.out.println("=== JPA Relationships Demo ===\n");
        SpringApplication.run(JpaRelationshipsDemo.class, args);
    }
    
    @Bean
    public CommandLineRunner demo(UserAccountRepository userRepository,
                                 BlogPostRepository postRepository,
                                 CategoryRepository categoryRepository,
                                 OrderEntityRepository orderRepository) {
        return (args) -> {
            System.out.println("🚀 JPA Relationships Demo Started!\n");
            
            // One-to-One relationship
            demonstrateOneToOne(userRepository);
            
            // One-to-Many relationship
            demonstrateOneToMany(postRepository, userRepository);
            
            // Many-to-Many relationship
            demonstrateManyToMany(postRepository, categoryRepository);
            
            // Complex relationship example
            demonstrateComplexRelationships(orderRepository);
            
            System.out.println("\n✅ Relationships demo completed!");
        };
    }
    
    @Transactional
    private void demonstrateOneToOne(UserAccountRepository userRepository) {
        System.out.println("1. One-to-One Relationship (User ↔ Profile):\n");
        
        // Create user with profile
        UserAccount user = new UserAccount();
        user.setUsername("john_doe");
        user.setEmail("john@example.com");
        user.setPassword("securepassword");
        
        UserProfile profile = new UserProfile();
        profile.setFullName("John Doe");
        profile.setBio("Software developer passionate about Java");
        profile.setAvatarUrl("https://example.com/avatar.jpg");
        profile.setWebsite("https://johndoe.com");
        
        // Bidirectional relationship
        user.setProfile(profile);
        profile.setUser(user);
        
        UserAccount savedUser = userRepository.save(user);
        System.out.println("✅ User saved with profile");
        System.out.println("   Username: " + savedUser.getUsername());
        System.out.println("   Profile: " + savedUser.getProfile().getFullName());
        
        // Fetch user with profile
        UserAccount fetchedUser = userRepository.findById(savedUser.getId()).orElseThrow();
        System.out.println("🔍 Fetched user profile: " + fetchedUser.getProfile().getBio());
    }
    
    @Transactional
    private void demonstrateOneToMany(BlogPostRepository postRepository, 
                                     UserAccountRepository userRepository) {
        System.out.println("\n2. One-to-Many Relationship (User → Posts, Post → Comments):\n");
        
        // Get or create user
        UserAccount author = userRepository.findByUsername("john_doe");
        
        // Create blog post
        BlogPost post = new BlogPost();
        post.setTitle("Understanding JPA Relationships");
        post.setContent("JPA provides powerful relationship mapping capabilities...");
        post.setAuthor(author);
        
        // Add comments
        Comment comment1 = new Comment();
        comment1.setAuthor("Alice");
        comment1.setContent("Great article!");
        comment1.setPost(post);
        
        Comment comment2 = new Comment();
        comment2.setAuthor("Bob");
        comment2.setContent("Very helpful, thanks!");
        comment2.setPost(post);
        
        post.getComments().add(comment1);
        post.getComments().add(comment2);
        
        BlogPost savedPost = postRepository.save(post);
        System.out.println("✅ Blog post saved with comments");
        System.out.println("   Title: " + savedPost.getTitle());
        System.out.println("   Author: " + savedPost.getAuthor().getUsername());
        System.out.println("   Comments: " + savedPost.getComments().size());
        
        // Fetch and display comments
        BlogPost fetchedPost = postRepository.findById(savedPost.getId()).orElseThrow();
        System.out.println("📝 Comments on post:");
        fetchedPost.getComments().forEach(comment ->
            System.out.println("   - " + comment.getAuthor() + ": " + comment.getContent())
        );
    }
    
    @Transactional
    private void demonstrateManyToMany(BlogPostRepository postRepository,
                                      CategoryRepository categoryRepository) {
        System.out.println("\n3. Many-to-Many Relationship (Posts ↔ Categories):\n");
        
        // Create categories
        Category java = new Category("Java", "Java programming language");
        Category spring = new Category("Spring", "Spring framework");
        Category jpa = new Category("JPA", "Java Persistence API");
        
        categoryRepository.saveAll(Arrays.asList(java, spring, jpa));
        
        // Create posts with categories
        BlogPost post1 = postRepository.findByTitle("Understanding JPA Relationships");
        post1.getCategories().add(java);
        post1.getCategories().add(jpa);
        postRepository.save(post1);
        
        BlogPost post2 = new BlogPost();
        post2.setTitle("Spring Boot Best Practices");
        post2.setContent("Learn the best practices for Spring Boot development...");
        post2.setAuthor(post1.getAuthor());
        post2.getCategories().add(java);
        post2.getCategories().add(spring);
        
        postRepository.save(post2);
        
        System.out.println("✅ Posts linked to categories");
        
        // Find posts by category
        List<BlogPost> javaPosts = postRepository.findByCategoriesName("Java");
        System.out.println("☕ Posts in Java category: " + javaPosts.size());
        javaPosts.forEach(post ->
            System.out.println("   - " + post.getTitle())
        );
        
        // Find categories with post count
        List<Object[]> categoryStats = categoryRepository.findCategoriesWithPostCount();
        System.out.println("📊 Category statistics:");
        categoryStats.forEach(stat ->
            System.out.println("   - " + stat[0] + ": " + stat[1] + " posts")
        );
    }
    
    @Transactional
    private void demonstrateComplexRelationships(OrderEntityRepository orderRepository) {
        System.out.println("\n4. Complex Relationships (Order System):\n");
        
        // Create customer
        CustomerEntity customer = new CustomerEntity();
        customer.setName("John Doe");
        customer.setEmail("john@example.com");
        
        // Create products
        ProductEntity laptop = new ProductEntity("Laptop", new BigDecimal("999.99"), 10);
        ProductEntity mouse = new ProductEntity("Mouse", new BigDecimal("29.99"), 100);
        
        // Create order
        OrderEntity order = new OrderEntity();
        order.setOrderNumber("ORD-001");
        order.setCustomer(customer);
        
        // Add order items
        OrderItem item1 = new OrderItem();
        item1.setOrder(order);
        item1.setProduct(laptop);
        item1.setQuantity(1);
        item1.setPrice(laptop.getPrice());
        
        OrderItem item2 = new OrderItem();
        item2.setOrder(order);
        item2.setProduct(mouse);
        item2.setQuantity(2);
        item2.setPrice(mouse.getPrice());
        
        order.getItems().add(item1);
        order.getItems().add(item2);
        order.calculateTotalAmount();
        
        // Add to customer orders
        customer.getOrders().add(order);
        
        OrderEntity savedOrder = orderRepository.save(order);
        System.out.println("✅ Order saved with relationships");
        System.out.println("   Order: " + savedOrder.getOrderNumber());
        System.out.println("   Customer: " + savedOrder.getCustomer().getName());
        System.out.println("   Items: " + savedOrder.getItems().size());
        System.out.println("   Total: $" + savedOrder.getTotalAmount());
        
        // Fetch order with all relationships
        OrderEntity fetchedOrder = orderRepository.findOrderWithDetails(savedOrder.getId());
        System.out.println("📦 Order details:");
        fetchedOrder.getItems().forEach(item ->
            System.out.println("   - " + item.getProduct().getName() + 
                             " x" + item.getQuantity() + 
                             " = $" + item.getSubtotal())
        );
    }
}

// User and Profile entities (One-to-One)
@Entity
@Table(name = "user_accounts")
class UserAccount {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, 
              fetch = FetchType.LAZY, optional = false)
    private UserProfile profile;
    
    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BlogPost> posts = new ArrayList<>();
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public UserProfile getProfile() { return profile; }
    public void setProfile(UserProfile profile) { this.profile = profile; }
    
    public List<BlogPost> getPosts() { return posts; }
    public void setPosts(List<BlogPost> posts) { this.posts = posts; }
}

@Entity
@Table(name = "user_profiles")
class UserProfile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "full_name")
    private String fullName;
    
    @Column(columnDefinition = "TEXT")
    private String bio;
    
    @Column(name = "avatar_url")
    private String avatarUrl;
    
    private String website;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    
    public UserAccount getUser() { return user; }
    public void setUser(UserAccount user) { this.user = user; }
}

// Blog Post and Comments (One-to-Many)
@Entity
@Table(name = "blog_posts")
class BlogPost {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private UserAccount author;
    
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, 
               orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();
    
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "post_categories",
               joinColumns = @JoinColumn(name = "post_id"),
               inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<Category> categories = new HashSet<>();
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public UserAccount getAuthor() { return author; }
    public void setAuthor(UserAccount author) { this.author = author; }
    
    public List<Comment> getComments() { return comments; }
    public void setComments(List<Comment> comments) { this.comments = comments; }
    
    public Set<Category> getCategories() { return categories; }
    public void setCategories(Set<Category> categories) { this.categories = categories; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

@Entity
@Table(name = "comments")
class Comment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String author;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private BlogPost post;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public BlogPost getPost() { return post; }
    public void setPost(BlogPost post) { this.post = post; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

// Category (Many-to-Many)
@Entity
@Table(name = "categories")
class Category {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String name;
    
    private String description;
    
    @ManyToMany(mappedBy = "categories")
    private Set<BlogPost> posts = new HashSet<>();
    
    public Category() {}
    
    public Category(String name, String description) {
        this.name = name;
        this.description = description;
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Set<BlogPost> getPosts() { return posts; }
    public void setPosts(Set<BlogPost> posts) { this.posts = posts; }
}

// Complex relationship example (Order system)
@Entity
@Table(name = "customers")
class CustomerEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String email;
    
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderEntity> orders = new ArrayList<>();
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public List<OrderEntity> getOrders() { return orders; }
    public void setOrders(List<OrderEntity> orders) { this.orders = orders; }
}

@Entity
@Table(name = "products_catalog")
class ProductEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private BigDecimal price;
    private Integer stock;
    
    public ProductEntity() {}
    
    public ProductEntity(String name, BigDecimal price, Integer stock) {
        this.name = name;
        this.price = price;
        this.stock = stock;
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
}

@Entity
@Table(name = "orders_catalog")
class OrderEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "order_number", unique = true)
    private String orderNumber;
    
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "customer_id")
    private CustomerEntity customer;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
    
    @Column(name = "total_amount")
    private BigDecimal totalAmount;
    
    @Column(name = "order_date")
    private LocalDateTime orderDate = LocalDateTime.now();
    
    // Business logic
    public void calculateTotalAmount() {
        this.totalAmount = items.stream()
            .map(OrderItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    
    public CustomerEntity getCustomer() { return customer; }
    public void setCustomer(CustomerEntity customer) { this.customer = customer; }
    
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }
}

@Entity
@Table(name = "order_items")
class OrderItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "order_id")
    private OrderEntity order;
    
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "product_id")
    private ProductEntity product;
    
    private Integer quantity;
    private BigDecimal price;
    
    public BigDecimal getSubtotal() {
        return price.multiply(new BigDecimal(quantity));
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public OrderEntity getOrder() { return order; }
    public void setOrder(OrderEntity order) { this.order = order; }
    
    public ProductEntity getProduct() { return product; }
    public void setProduct(ProductEntity product) { this.product = product; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}

// Repositories for relationships
@Repository
interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    UserAccount findByUsername(String username);
}

@Repository
interface BlogPostRepository extends JpaRepository<BlogPost, Long> {
    
    BlogPost findByTitle(String title);
    
    @Query("SELECT p FROM BlogPost p JOIN p.categories c WHERE c.name = :categoryName")
    List<BlogPost> findByCategoriesName(@Param("categoryName") String categoryName);
}

@Repository
interface CategoryRepository extends JpaRepository<Category, Long> {
    
    @Query("SELECT c.name, COUNT(p) FROM Category c LEFT JOIN c.posts p GROUP BY c.name")
    List<Object[]> findCategoriesWithPostCount();
}

@Repository
interface OrderEntityRepository extends JpaRepository<OrderEntity, Long> {
    
    @Query("SELECT o FROM OrderEntity o " +
           "LEFT JOIN FETCH o.customer " +
           "LEFT JOIN FETCH o.items i " +
           "LEFT JOIN FETCH i.product " +
           "WHERE o.id = :id")
    OrderEntity findOrderWithDetails(@Param("id") Long id);
}
```

---

## 🎯 Part 3: Advanced Queries and Transactions (30 minutes)

### **Advanced Query Techniques**

Spring Data JPA provides various ways to create complex queries including JPQL, native SQL, and specifications.

### **Advanced Queries Demo**

Create `AdvancedQueriesDemo.java`:

```java
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Advanced queries and transactions demonstration
 * Shows specifications, projections, and transaction management
 */
@SpringBootApplication
public class AdvancedQueriesDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Advanced Queries Demo ===\n");
        SpringApplication.run(AdvancedQueriesDemo.class, args);
    }
    
    @Bean
    public CommandLineRunner demo(EmployeeRepository employeeRepository,
                                 TransactionService transactionService) {
        return (args) -> {
            System.out.println("🚀 Advanced Queries Demo Started!\n");
            
            // Initialize data
            initializeEmployees(employeeRepository);
            
            // Demonstrate pagination and sorting
            demonstratePaginationAndSorting(employeeRepository);
            
            // Demonstrate specifications
            demonstrateSpecifications(employeeRepository);
            
            // Demonstrate projections
            demonstrateProjections(employeeRepository);
            
            // Demonstrate transactions
            demonstrateTransactions(transactionService);
            
            System.out.println("\n✅ Advanced queries demo completed!");
        };
    }
    
    private void initializeEmployees(EmployeeRepository repository) {
        List<Employee> employees = Arrays.asList(
            new Employee("John", "Doe", "john@example.com", "Engineering", 
                        new BigDecimal("85000"), LocalDate.of(2020, 1, 15)),
            new Employee("Jane", "Smith", "jane@example.com", "Marketing", 
                        new BigDecimal("75000"), LocalDate.of(2019, 6, 1)),
            new Employee("Bob", "Johnson", "bob@example.com", "Engineering", 
                        new BigDecimal("95000"), LocalDate.of(2018, 3, 20)),
            new Employee("Alice", "Brown", "alice@example.com", "HR", 
                        new BigDecimal("65000"), LocalDate.of(2021, 9, 10)),
            new Employee("Charlie", "Wilson", "charlie@example.com", "Engineering", 
                        new BigDecimal("105000"), LocalDate.of(2017, 11, 5)),
            new Employee("Diana", "Davis", "diana@example.com", "Marketing", 
                        new BigDecimal("80000"), LocalDate.of(2020, 7, 12))
        );
        
        repository.saveAll(employees);
        System.out.println("✅ Initialized " + employees.size() + " employees");
    }
    
    private void demonstratePaginationAndSorting(EmployeeRepository repository) {
        System.out.println("\n1. Pagination and Sorting:\n");
        
        // Pagination
        Pageable pageable = PageRequest.of(0, 3, Sort.by("salary").descending());
        Page<Employee> page = repository.findAll(pageable);
        
        System.out.println("📄 Page 1 (3 items per page, sorted by salary DESC):");
        System.out.println("   Total elements: " + page.getTotalElements());
        System.out.println("   Total pages: " + page.getTotalPages());
        System.out.println("   Current page: " + (page.getNumber() + 1));
        
        page.getContent().forEach(emp ->
            System.out.println("   - " + emp.getFullName() + ": $" + emp.getSalary())
        );
        
        // Next page
        Page<Employee> nextPage = repository.findAll(page.nextPageable());
        System.out.println("\n📄 Page 2:");
        nextPage.getContent().forEach(emp ->
            System.out.println("   - " + emp.getFullName() + ": $" + emp.getSalary())
        );
        
        // Department-specific pagination
        Page<Employee> engineeringPage = repository.findByDepartment("Engineering", 
                                                                    PageRequest.of(0, 2));
        System.out.println("\n🔧 Engineering department (page 1):");
        engineeringPage.getContent().forEach(emp ->
            System.out.println("   - " + emp.getFullName())
        );
    }
    
    private void demonstrateSpecifications(EmployeeRepository repository) {
        System.out.println("\n2. Specifications (Dynamic Queries):\n");
        
        // Single specification
        Specification<Employee> highSalary = (root, query, cb) ->
            cb.greaterThan(root.get("salary"), new BigDecimal("80000"));
        
        List<Employee> highEarners = repository.findAll(highSalary);
        System.out.println("💰 High earners (>$80k): " + highEarners.size());
        
        // Combined specifications
        Specification<Employee> engineeringDept = (root, query, cb) ->
            cb.equal(root.get("department"), "Engineering");
        
        Specification<Employee> recentHire = (root, query, cb) ->
            cb.greaterThan(root.get("hireDate"), LocalDate.of(2019, 1, 1));
        
        List<Employee> recentEngineers = repository.findAll(
            Specification.where(engineeringDept).and(recentHire)
        );
        System.out.println("🔧 Recent engineering hires: " + recentEngineers.size());
        recentEngineers.forEach(emp ->
            System.out.println("   - " + emp.getFullName() + " (hired: " + emp.getHireDate() + ")")
        );
        
        // Dynamic search
        EmployeeSearchCriteria criteria = new EmployeeSearchCriteria();
        criteria.setDepartment("Engineering");
        criteria.setMinSalary(new BigDecimal("90000"));
        
        List<Employee> searchResults = repository.findAll(buildSpecification(criteria));
        System.out.println("\n🔍 Dynamic search results:");
        searchResults.forEach(emp ->
            System.out.println("   - " + emp.getFullName() + ": $" + emp.getSalary())
        );
    }
    
    private Specification<Employee> buildSpecification(EmployeeSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (criteria.getDepartment() != null) {
                predicates.add(cb.equal(root.get("department"), criteria.getDepartment()));
            }
            
            if (criteria.getMinSalary() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("salary"), criteria.getMinSalary()));
            }
            
            if (criteria.getMaxSalary() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("salary"), criteria.getMaxSalary()));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    private void demonstrateProjections(EmployeeRepository repository) {
        System.out.println("\n3. Projections:\n");
        
        // Interface-based projection
        List<EmployeeNameOnly> names = repository.findAllProjectedBy();
        System.out.println("👤 Employee names (interface projection):");
        names.forEach(emp ->
            System.out.println("   - " + emp.getFirstName() + " " + emp.getLastName())
        );
        
        // Class-based projection (DTO)
        List<EmployeeDTO> dtos = repository.findAllDTOs();
        System.out.println("\n📋 Employee DTOs (class projection):");
        dtos.forEach(dto ->
            System.out.println("   - " + dto.getFullName() + " (" + dto.getDepartment() + ")")
        );
        
        // Dynamic projection
        List<EmployeeSummary> summaries = repository.findByDepartment("Engineering", EmployeeSummary.class);
        System.out.println("\n📊 Engineering summaries (dynamic projection):");
        summaries.forEach(summary ->
            System.out.println("   - " + summary.getFullName() + ": $" + summary.getSalary())
        );
    }
    
    private void demonstrateTransactions(TransactionService service) {
        System.out.println("\n4. Transaction Management:\n");
        
        try {
            // Successful transaction
            service.performSalaryAdjustment("Engineering", new BigDecimal("1.05"));
            System.out.println("✅ Salary adjustment completed successfully");
            
            // Failed transaction (will rollback)
            service.performBulkUpdate();
        } catch (Exception e) {
            System.out.println("❌ Transaction rolled back: " + e.getMessage());
        }
    }
}

// Employee entity for advanced queries
@Entity
@Table(name = "employees")
class Employee {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "first_name")
    private String firstName;
    
    @Column(name = "last_name")
    private String lastName;
    
    private String email;
    private String department;
    private BigDecimal salary;
    
    @Column(name = "hire_date")
    private LocalDate hireDate;
    
    @Version
    private Long version;
    
    public Employee() {}
    
    public Employee(String firstName, String lastName, String email, 
                   String department, BigDecimal salary, LocalDate hireDate) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.department = department;
        this.salary = salary;
        this.hireDate = hireDate;
    }
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public BigDecimal getSalary() { return salary; }
    public void setSalary(BigDecimal salary) { this.salary = salary; }
    
    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}

// Repository with advanced features
@Repository
interface EmployeeRepository extends JpaRepository<Employee, Long>, 
                                   JpaSpecificationExecutor<Employee> {
    
    Page<Employee> findByDepartment(String department, Pageable pageable);
    
    @Query("SELECT e FROM Employee e WHERE e.salary > :salary ORDER BY e.salary DESC")
    List<Employee> findHighEarners(@Param("salary") BigDecimal salary);
    
    // Interface-based projection
    List<EmployeeNameOnly> findAllProjectedBy();
    
    // Class-based projection (DTO)
    @Query("SELECT new com.example.EmployeeDTO(e.firstName, e.lastName, e.department) FROM Employee e")
    List<EmployeeDTO> findAllDTOs();
    
    // Dynamic projection
    <T> List<T> findByDepartment(String department, Class<T> type);
    
    @Modifying
    @Query("UPDATE Employee e SET e.salary = e.salary * :multiplier WHERE e.department = :department")
    int updateSalariesByDepartment(@Param("department") String department, 
                                  @Param("multiplier") BigDecimal multiplier);
}

// Projections
interface EmployeeNameOnly {
    String getFirstName();
    String getLastName();
}

interface EmployeeSummary {
    String getFirstName();
    String getLastName();
    BigDecimal getSalary();
    
    default String getFullName() {
        return getFirstName() + " " + getLastName();
    }
}

// DTO for class-based projection
class EmployeeDTO {
    private String firstName;
    private String lastName;
    private String department;
    
    public EmployeeDTO(String firstName, String lastName, String department) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.department = department;
    }
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    // Getters
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getDepartment() { return department; }
}

// Search criteria
class EmployeeSearchCriteria {
    private String department;
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    
    // Getters and setters
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public BigDecimal getMinSalary() { return minSalary; }
    public void setMinSalary(BigDecimal minSalary) { this.minSalary = minSalary; }
    
    public BigDecimal getMaxSalary() { return maxSalary; }
    public void setMaxSalary(BigDecimal maxSalary) { this.maxSalary = maxSalary; }
}

// Transaction service
@org.springframework.stereotype.Service
@Transactional
class TransactionService {
    
    @org.springframework.beans.factory.annotation.Autowired
    private EmployeeRepository employeeRepository;
    
    @Transactional
    public void performSalaryAdjustment(String department, BigDecimal multiplier) {
        System.out.println("💼 Performing salary adjustment for " + department);
        
        int updated = employeeRepository.updateSalariesByDepartment(department, multiplier);
        System.out.println("   Updated " + updated + " employees");
        
        // Additional business logic
        List<Employee> employees = employeeRepository.findAll();
        BigDecimal totalSalary = employees.stream()
            .map(Employee::getSalary)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        System.out.println("   Total salary budget: $" + totalSalary);
    }
    
    @Transactional(rollbackFor = Exception.class)
    public void performBulkUpdate() throws Exception {
        System.out.println("📦 Performing bulk update...");
        
        // This will cause rollback
        throw new RuntimeException("Simulated error - transaction will rollback");
    }
}
```

---

## 🏗️ Part 4: Complete E-commerce System (30 minutes)

### **E-commerce Application with Spring Data JPA**

Let's build a complete e-commerce system demonstrating all JPA concepts.

Create `EcommerceApplication.java`:

```java
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Complete E-commerce System with Spring Data JPA
 * Demonstrates real-world application with complex relationships
 */
@SpringBootApplication
public class EcommerceApplication {
    
    public static void main(String[] args) {
        System.out.println("=== E-commerce Application with Spring Data JPA ===\n");
        SpringApplication.run(EcommerceApplication.class, args);
    }
    
    @Bean
    public CommandLineRunner demo(EcommerceService ecommerceService) {
        return (args) -> {
            System.out.println("🛒 E-commerce System Started!\n");
            
            // Initialize data
            ecommerceService.initializeData();
            
            // Customer operations
            demonstrateCustomerOperations(ecommerceService);
            
            // Product catalog
            demonstrateProductCatalog(ecommerceService);
            
            // Shopping cart
            demonstrateShoppingCart(ecommerceService);
            
            // Order processing
            demonstrateOrderProcessing(ecommerceService);
            
            // Reports and analytics
            demonstrateReportsAndAnalytics(ecommerceService);
            
            System.out.println("\n✅ E-commerce demo completed!");
        };
    }
    
    private void demonstrateCustomerOperations(EcommerceService service) {
        System.out.println("1. Customer Operations:\n");
        
        // Register customer
        EcommerceCustomer customer = service.registerCustomer(
            "John Doe", "john@example.com", "password123",
            "123 Main St", "Springfield", "IL", "62701", "USA"
        );
        System.out.println("✅ Customer registered: " + customer.getFullName());
        
        // Update customer
        customer.setPhoneNumber("+1-555-0123");
        service.updateCustomer(customer);
        System.out.println("✅ Customer updated");
        
        // Find customers
        List<EcommerceCustomer> customers = service.findCustomersByCity("Springfield");
        System.out.println("🏠 Customers in Springfield: " + customers.size());
    }
    
    private void demonstrateProductCatalog(EcommerceService service) {
        System.out.println("\n2. Product Catalog:\n");
        
        // Browse products
        Page<EcommerceProduct> products = service.browseProducts(0, 5);
        System.out.println("📦 Products (page 1):");
        products.getContent().forEach(p ->
            System.out.println("   - " + p.getName() + ": $" + p.getPrice())
        );
        
        // Search products
        List<EcommerceProduct> searchResults = service.searchProducts("Laptop");
        System.out.println("\n🔍 Search results for 'Laptop': " + searchResults.size());
        
        // Filter by category
        List<EcommerceProduct> electronics = service.getProductsByCategory("Electronics");
        System.out.println("📱 Electronics products: " + electronics.size());
    }
    
    private void demonstrateShoppingCart(EcommerceService service) {
        System.out.println("\n3. Shopping Cart:\n");
        
        EcommerceCustomer customer = service.findCustomerByEmail("john@example.com");
        EcommerceProduct laptop = service.findProductByName("Gaming Laptop");
        EcommerceProduct mouse = service.findProductByName("Wireless Mouse");
        
        // Add to cart
        ShoppingCart cart = service.getOrCreateCart(customer);
        service.addToCart(cart, laptop, 1);
        service.addToCart(cart, mouse, 2);
        
        System.out.println("🛒 Shopping cart:");
        cart.getItems().forEach(item ->
            System.out.println("   - " + item.getProduct().getName() + 
                             " x" + item.getQuantity() + 
                             " = $" + item.getSubtotal())
        );
        System.out.println("   Total: $" + cart.getTotalAmount());
    }
    
    private void demonstrateOrderProcessing(EcommerceService service) {
        System.out.println("\n4. Order Processing:\n");
        
        EcommerceCustomer customer = service.findCustomerByEmail("john@example.com");
        ShoppingCart cart = service.getActiveCart(customer);
        
        // Place order
        EcommerceOrder order = service.placeOrder(customer, cart);
        System.out.println("✅ Order placed: " + order.getOrderNumber());
        System.out.println("   Status: " + order.getStatus());
        System.out.println("   Total: $" + order.getTotalAmount());
        
        // Process payment
        service.processPayment(order, "CREDIT_CARD", "4111111111111111");
        System.out.println("💳 Payment processed");
        
        // Ship order
        service.shipOrder(order, "FedEx", "TRACK123456");
        System.out.println("📦 Order shipped");
    }
    
    private void demonstrateReportsAndAnalytics(EcommerceService service) {
        System.out.println("\n5. Reports and Analytics:\n");
        
        // Top selling products
        List<Object[]> topProducts = service.getTopSellingProducts(3);
        System.out.println("🏆 Top selling products:");
        topProducts.forEach(result ->
            System.out.println("   - " + result[0] + ": " + result[1] + " sold")
        );
        
        // Customer statistics
        CustomerStatistics stats = service.getCustomerStatistics("john@example.com");
        System.out.println("\n📊 Customer statistics:");
        System.out.println("   Orders: " + stats.getTotalOrders());
        System.out.println("   Total spent: $" + stats.getTotalSpent());
        System.out.println("   Average order: $" + stats.getAverageOrderValue());
        
        // Revenue report
        BigDecimal totalRevenue = service.getTotalRevenue();
        System.out.println("\n💰 Total revenue: $" + totalRevenue);
    }
}

// E-commerce service
@Service
@Transactional
class EcommerceService {
    
    @Autowired
    private EcommerceCustomerRepository customerRepository;
    
    @Autowired
    private EcommerceProductRepository productRepository;
    
    @Autowired
    private EcommerceCategoryRepository categoryRepository;
    
    @Autowired
    private ShoppingCartRepository cartRepository;
    
    @Autowired
    private EcommerceOrderRepository orderRepository;
    
    // Initialize sample data
    public void initializeData() {
        // Categories
        EcommerceCategory electronics = new EcommerceCategory("Electronics", "Electronic devices and accessories");
        EcommerceCategory computers = new EcommerceCategory("Computers", "Computers and components");
        EcommerceCategory accessories = new EcommerceCategory("Accessories", "Computer accessories");
        
        categoryRepository.saveAll(Arrays.asList(electronics, computers, accessories));
        
        // Products
        List<EcommerceProduct> products = Arrays.asList(
            createProduct("Gaming Laptop", "High-performance gaming laptop", 
                         new BigDecimal("1499.99"), computers, 25),
            createProduct("Business Laptop", "Professional business laptop", 
                         new BigDecimal("999.99"), computers, 50),
            createProduct("Wireless Mouse", "Ergonomic wireless mouse", 
                         new BigDecimal("39.99"), accessories, 200),
            createProduct("Mechanical Keyboard", "RGB mechanical keyboard", 
                         new BigDecimal("129.99"), accessories, 100),
            createProduct("4K Monitor", "27-inch 4K IPS monitor", 
                         new BigDecimal("399.99"), electronics, 75)
        );
        
        productRepository.saveAll(products);
        
        // Sample customers
        registerCustomer("Alice Johnson", "alice@example.com", "password",
                        "456 Oak St", "Springfield", "IL", "62702", "USA");
        registerCustomer("Bob Smith", "bob@example.com", "password",
                        "789 Pine St", "Chicago", "IL", "60601", "USA");
    }
    
    // Customer operations
    public EcommerceCustomer registerCustomer(String name, String email, String password,
                                            String street, String city, String state, 
                                            String zipCode, String country) {
        EcommerceCustomer customer = new EcommerceCustomer();
        customer.setFullName(name);
        customer.setEmail(email);
        customer.setPassword(password); // Should be encrypted in production
        
        ShippingAddress address = new ShippingAddress();
        address.setStreet(street);
        address.setCity(city);
        address.setState(state);
        address.setZipCode(zipCode);
        address.setCountry(country);
        
        customer.getAddresses().add(address);
        customer.setDefaultAddress(address);
        
        return customerRepository.save(customer);
    }
    
    public EcommerceCustomer updateCustomer(EcommerceCustomer customer) {
        return customerRepository.save(customer);
    }
    
    public EcommerceCustomer findCustomerByEmail(String email) {
        return customerRepository.findByEmail(email);
    }
    
    public List<EcommerceCustomer> findCustomersByCity(String city) {
        return customerRepository.findByAddressesCity(city);
    }
    
    // Product operations
    public Page<EcommerceProduct> browseProducts(int page, int size) {
        return productRepository.findAll(PageRequest.of(page, size));
    }
    
    public List<EcommerceProduct> searchProducts(String keyword) {
        return productRepository.searchProducts(keyword);
    }
    
    public List<EcommerceProduct> getProductsByCategory(String categoryName) {
        return productRepository.findByCategoryName(categoryName);
    }
    
    public EcommerceProduct findProductByName(String name) {
        return productRepository.findByName(name);
    }
    
    // Shopping cart operations
    public ShoppingCart getOrCreateCart(EcommerceCustomer customer) {
        ShoppingCart cart = cartRepository.findByCustomerAndStatus(customer, CartStatus.ACTIVE);
        if (cart == null) {
            cart = new ShoppingCart();
            cart.setCustomer(customer);
            cart = cartRepository.save(cart);
        }
        return cart;
    }
    
    public ShoppingCart getActiveCart(EcommerceCustomer customer) {
        return cartRepository.findByCustomerAndStatus(customer, CartStatus.ACTIVE);
    }
    
    public void addToCart(ShoppingCart cart, EcommerceProduct product, int quantity) {
        CartItem existingItem = cart.getItems().stream()
            .filter(item -> item.getProduct().equals(product))
            .findFirst()
            .orElse(null);
        
        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
        } else {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(quantity);
            item.setPrice(product.getPrice());
            cart.getItems().add(item);
        }
        
        cart.updateTotalAmount();
        cartRepository.save(cart);
    }
    
    // Order operations
    public EcommerceOrder placeOrder(EcommerceCustomer customer, ShoppingCart cart) {
        EcommerceOrder order = new EcommerceOrder();
        order.setOrderNumber("ORD-" + System.currentTimeMillis());
        order.setCustomer(customer);
        order.setShippingAddress(customer.getDefaultAddress());
        
        // Convert cart items to order items
        cart.getItems().forEach(cartItem -> {
            OrderLineItem orderItem = new OrderLineItem();
            orderItem.setOrder(order);
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getPrice());
            order.getItems().add(orderItem);
            
            // Update product stock
            EcommerceProduct product = cartItem.getProduct();
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);
        });
        
        order.calculateTotalAmount();
        order.setStatus(EcommerceOrderStatus.PENDING);
        
        // Mark cart as converted
        cart.setStatus(CartStatus.CONVERTED);
        cartRepository.save(cart);
        
        return orderRepository.save(order);
    }
    
    public void processPayment(EcommerceOrder order, String paymentMethod, String paymentDetails) {
        // Process payment (simplified)
        order.setPaymentMethod(paymentMethod);
        order.setPaymentDate(LocalDateTime.now());
        order.setStatus(EcommerceOrderStatus.PAID);
        orderRepository.save(order);
    }
    
    public void shipOrder(EcommerceOrder order, String carrier, String trackingNumber) {
        order.setStatus(EcommerceOrderStatus.SHIPPED);
        order.setShippingCarrier(carrier);
        order.setTrackingNumber(trackingNumber);
        order.setShippingDate(LocalDateTime.now());
        orderRepository.save(order);
    }
    
    // Analytics
    public List<Object[]> getTopSellingProducts(int limit) {
        return orderRepository.findTopSellingProducts(PageRequest.of(0, limit));
    }
    
    public CustomerStatistics getCustomerStatistics(String email) {
        return orderRepository.getCustomerStatistics(email);
    }
    
    public BigDecimal getTotalRevenue() {
        return orderRepository.getTotalRevenue();
    }
    
    private EcommerceProduct createProduct(String name, String description, 
                                         BigDecimal price, EcommerceCategory category, int stock) {
        EcommerceProduct product = new EcommerceProduct();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setCategory(category);
        product.setStockQuantity(stock);
        product.setActive(true);
        return product;
    }
}

// E-commerce entities
@Entity
@Table(name = "ecommerce_customers")
class EcommerceCustomer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "full_name", nullable = false)
    private String fullName;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Column(name = "phone_number")
    private String phoneNumber;
    
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "customer_id")
    private List<ShippingAddress> addresses = new ArrayList<>();
    
    @ManyToOne
    @JoinColumn(name = "default_address_id")
    private ShippingAddress defaultAddress;
    
    @OneToMany(mappedBy = "customer")
    private List<EcommerceOrder> orders = new ArrayList<>();
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    
    public List<ShippingAddress> getAddresses() { return addresses; }
    public void setAddresses(List<ShippingAddress> addresses) { this.addresses = addresses; }
    
    public ShippingAddress getDefaultAddress() { return defaultAddress; }
    public void setDefaultAddress(ShippingAddress defaultAddress) { this.defaultAddress = defaultAddress; }
    
    public List<EcommerceOrder> getOrders() { return orders; }
    public void setOrders(List<EcommerceOrder> orders) { this.orders = orders; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

@Entity
@Table(name = "shipping_addresses")
class ShippingAddress {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String street;
    private String city;
    private String state;
    
    @Column(name = "zip_code")
    private String zipCode;
    
    private String country;
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    
    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }
    
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
}

@Entity
@Table(name = "ecommerce_categories")
class EcommerceCategory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String name;
    
    private String description;
    
    @OneToMany(mappedBy = "category")
    private List<EcommerceProduct> products = new ArrayList<>();
    
    public EcommerceCategory() {}
    
    public EcommerceCategory(String name, String description) {
        this.name = name;
        this.description = description;
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public List<EcommerceProduct> getProducts() { return products; }
    public void setProducts(List<EcommerceProduct> products) { this.products = products; }
}

@Entity
@Table(name = "ecommerce_products")
class EcommerceProduct {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private BigDecimal price;
    
    @Column(name = "stock_quantity")
    private Integer stockQuantity;
    
    @ManyToOne
    @JoinColumn(name = "category_id")
    private EcommerceCategory category;
    
    private Boolean active;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
    
    public EcommerceCategory getCategory() { return category; }
    public void setCategory(EcommerceCategory category) { this.category = category; }
    
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

@Entity
@Table(name = "shopping_carts")
class ShoppingCart {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private EcommerceCustomer customer;
    
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();
    
    @Column(name = "total_amount")
    private BigDecimal totalAmount = BigDecimal.ZERO;
    
    @Enumerated(EnumType.STRING)
    private CartStatus status = CartStatus.ACTIVE;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    public void updateTotalAmount() {
        this.totalAmount = items.stream()
            .map(item -> item.getPrice().multiply(new BigDecimal(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public EcommerceCustomer getCustomer() { return customer; }
    public void setCustomer(EcommerceCustomer customer) { this.customer = customer; }
    
    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public CartStatus getStatus() { return status; }
    public void setStatus(CartStatus status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

@Entity
@Table(name = "cart_items")
class CartItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "cart_id")
    private ShoppingCart cart;
    
    @ManyToOne
    @JoinColumn(name = "product_id")
    private EcommerceProduct product;
    
    private Integer quantity;
    private BigDecimal price;
    
    public BigDecimal getSubtotal() {
        return price.multiply(new BigDecimal(quantity));
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public ShoppingCart getCart() { return cart; }
    public void setCart(ShoppingCart cart) { this.cart = cart; }
    
    public EcommerceProduct getProduct() { return product; }
    public void setProduct(EcommerceProduct product) { this.product = product; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}

@Entity
@Table(name = "ecommerce_orders")
class EcommerceOrder {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "order_number", unique = true, nullable = false)
    private String orderNumber;
    
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private EcommerceCustomer customer;
    
    @ManyToOne
    @JoinColumn(name = "shipping_address_id")
    private ShippingAddress shippingAddress;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLineItem> items = new ArrayList<>();
    
    @Column(name = "total_amount")
    private BigDecimal totalAmount;
    
    @Enumerated(EnumType.STRING)
    private EcommerceOrderStatus status;
    
    @Column(name = "payment_method")
    private String paymentMethod;
    
    @Column(name = "payment_date")
    private LocalDateTime paymentDate;
    
    @Column(name = "shipping_carrier")
    private String shippingCarrier;
    
    @Column(name = "tracking_number")
    private String trackingNumber;
    
    @Column(name = "shipping_date")
    private LocalDateTime shippingDate;
    
    @Column(name = "order_date")
    private LocalDateTime orderDate = LocalDateTime.now();
    
    public void calculateTotalAmount() {
        this.totalAmount = items.stream()
            .map(OrderLineItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    
    public EcommerceCustomer getCustomer() { return customer; }
    public void setCustomer(EcommerceCustomer customer) { this.customer = customer; }
    
    public ShippingAddress getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(ShippingAddress shippingAddress) { this.shippingAddress = shippingAddress; }
    
    public List<OrderLineItem> getItems() { return items; }
    public void setItems(List<OrderLineItem> items) { this.items = items; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public EcommerceOrderStatus getStatus() { return status; }
    public void setStatus(EcommerceOrderStatus status) { this.status = status; }
    
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    
    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
    
    public String getShippingCarrier() { return shippingCarrier; }
    public void setShippingCarrier(String shippingCarrier) { this.shippingCarrier = shippingCarrier; }
    
    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    
    public LocalDateTime getShippingDate() { return shippingDate; }
    public void setShippingDate(LocalDateTime shippingDate) { this.shippingDate = shippingDate; }
    
    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }
}

@Entity
@Table(name = "order_line_items")
class OrderLineItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "order_id")
    private EcommerceOrder order;
    
    @ManyToOne
    @JoinColumn(name = "product_id")
    private EcommerceProduct product;
    
    private Integer quantity;
    private BigDecimal price;
    
    public BigDecimal getSubtotal() {
        return price.multiply(new BigDecimal(quantity));
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public EcommerceOrder getOrder() { return order; }
    public void setOrder(EcommerceOrder order) { this.order = order; }
    
    public EcommerceProduct getProduct() { return product; }
    public void setProduct(EcommerceProduct product) { this.product = product; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}

// Enums
enum CartStatus {
    ACTIVE, CONVERTED, ABANDONED
}

enum EcommerceOrderStatus {
    PENDING, PAID, PROCESSING, SHIPPED, DELIVERED, CANCELLED, REFUNDED
}

// Repositories
@Repository
interface EcommerceCustomerRepository extends JpaRepository<EcommerceCustomer, Long> {
    
    EcommerceCustomer findByEmail(String email);
    
    List<EcommerceCustomer> findByAddressesCity(String city);
}

@Repository
interface EcommerceProductRepository extends JpaRepository<EcommerceProduct, Long> {
    
    EcommerceProduct findByName(String name);
    
    List<EcommerceProduct> findByCategoryName(String categoryName);
    
    @Query("SELECT p FROM EcommerceProduct p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<EcommerceProduct> searchProducts(@Param("keyword") String keyword);
}

@Repository
interface EcommerceCategoryRepository extends JpaRepository<EcommerceCategory, Long> {
}

@Repository
interface ShoppingCartRepository extends JpaRepository<ShoppingCart, Long> {
    
    ShoppingCart findByCustomerAndStatus(EcommerceCustomer customer, CartStatus status);
}

@Repository
interface EcommerceOrderRepository extends JpaRepository<EcommerceOrder, Long> {
    
    List<EcommerceOrder> findByCustomer(EcommerceCustomer customer);
    
    @Query("SELECT i.product.name, SUM(i.quantity) as totalSold " +
           "FROM OrderLineItem i " +
           "GROUP BY i.product.name " +
           "ORDER BY totalSold DESC")
    List<Object[]> findTopSellingProducts(Pageable pageable);
    
    @Query("SELECT new com.example.CustomerStatistics(" +
           "COUNT(o), SUM(o.totalAmount), AVG(o.totalAmount)) " +
           "FROM EcommerceOrder o WHERE o.customer.email = :email")
    CustomerStatistics getCustomerStatistics(@Param("email") String email);
    
    @Query("SELECT SUM(o.totalAmount) FROM EcommerceOrder o WHERE o.status = 'PAID'")
    BigDecimal getTotalRevenue();
}

// Statistics class
class CustomerStatistics {
    private Long totalOrders;
    private BigDecimal totalSpent;
    private Double averageOrderValue;
    
    public CustomerStatistics(Long totalOrders, BigDecimal totalSpent, Double averageOrderValue) {
        this.totalOrders = totalOrders;
        this.totalSpent = totalSpent;
        this.averageOrderValue = averageOrderValue;
    }
    
    // Getters
    public Long getTotalOrders() { return totalOrders; }
    public BigDecimal getTotalSpent() { return totalSpent; }
    public Double getAverageOrderValue() { return averageOrderValue; }
}
```

---

## 🎯 Day 20 Summary

**Concepts Mastered:**
- ✅ **JPA Entity Mapping**: Entities, embeddables, and inheritance strategies
- ✅ **Spring Data Repositories**: CRUD operations and query methods
- ✅ **Entity Relationships**: One-to-One, One-to-Many, Many-to-Many mappings
- ✅ **Advanced Queries**: JPQL, native queries, specifications, and projections
- ✅ **Transaction Management**: ACID properties and optimistic locking
- ✅ **Complete E-commerce System**: Real-world application with complex data model

**Key Takeaways:**
1. **Spring Data JPA** simplifies database access with repository abstraction
2. **Entity mapping** provides object-relational mapping with annotations
3. **Relationships** model real-world associations between entities
4. **Query methods** enable database queries without writing SQL
5. **Transactions** ensure data consistency and integrity
6. **Specifications** provide dynamic query building capabilities

**Enterprise Benefits:**
- **Productivity**: Reduced boilerplate code with repository pattern
- **Type Safety**: Compile-time checking for queries and entities
- **Performance**: Lazy loading, caching, and query optimization
- **Maintainability**: Clear separation between data and business logic
- **Flexibility**: Support for multiple databases without code changes

**Next Steps:**
- Day 21: Week 3 Capstone Project - Complete enterprise application
- Advanced JPA topics: Auditing, events, and custom implementations
- Performance tuning: N+1 queries, batch operations, and caching
- Testing strategies: Repository testing and transactional tests

---

**🎉 Congratulations! You've mastered Spring Data JPA and built a complete e-commerce system with complex data relationships!**