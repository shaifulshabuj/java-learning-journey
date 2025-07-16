# Day 25: Frontend Integration (August 3, 2025)

**Date:** August 3, 2025  
**Duration:** 2.5 hours  
**Focus:** Master frontend integration with Thymeleaf, AJAX, WebSocket, and modern web technologies

---

## 🎯 Today's Learning Goals

By the end of this session, you'll:
- ✅ Master advanced Thymeleaf features and templating
- ✅ Implement AJAX integration with jQuery and Fetch API
- ✅ Handle forms with validation and error display
- ✅ Set up WebSocket for real-time communication
- ✅ Manage static resources and asset optimization
- ✅ Build a complete interactive web application

---

## 🎨 Part 1: Advanced Thymeleaf Features (45 minutes)

### **Thymeleaf Fundamentals**

**Thymeleaf** is a modern server-side Java template engine that provides:
- **Natural templates**: HTML files that can be opened in browsers
- **Spring integration**: Seamless integration with Spring MVC
- **Internationalization**: Built-in i18n support
- **Security**: XSS protection and CSRF support
- **Extensibility**: Custom dialects and processors

### **Advanced Thymeleaf Demo**

Create `ThymeleafAdvancedDemo.java`:

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Advanced Thymeleaf demonstration
 * Shows advanced templating features and Spring integration
 */
@SpringBootApplication
public class ThymeleafAdvancedDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Advanced Thymeleaf Demo ===\n");
        SpringApplication.run(ThymeleafAdvancedDemo.class, args);
        
        System.out.println("🎨 Thymeleaf Features Demonstrated:");
        System.out.println("   - Advanced expression language");
        System.out.println("   - Form handling and validation");
        System.out.println("   - Internationalization (i18n)");
        System.out.println("   - Template fragments and layouts");
        System.out.println("   - Custom utilities and dialects");
        System.out.println("   - Conditional rendering");
        System.out.println("   - Iteration and collections");
        System.out.println();
        
        System.out.println("🌐 Available Pages:");
        System.out.println("   http://localhost:8080/products");
        System.out.println("   http://localhost:8080/products/form");
        System.out.println("   http://localhost:8080/dashboard");
        System.out.println("   http://localhost:8080/internationalization");
        System.out.println();
    }
}

// Product model for demonstration
class Product {
    private Long id;
    
    @NotBlank(message = "Product name is required")
    @Size(min = 3, max = 100, message = "Product name must be between 3 and 100 characters")
    private String name;
    
    @NotBlank(message = "Description is required")
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @DecimalMax(value = "999999.99", message = "Price cannot exceed 999,999.99")
    private Double price;
    
    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stock;
    
    @NotBlank(message = "Category is required")
    private String category;
    
    private String imageUrl;
    private boolean featured;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public Product() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public Product(String name, String description, Double price, Integer stock, String category) {
        this();
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.category = category;
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    // Helper methods for templates
    public String getFormattedPrice() {
        return String.format("$%.2f", price);
    }
    
    public String getStockStatus() {
        if (stock == 0) return "Out of Stock";
        if (stock < 10) return "Low Stock";
        return "In Stock";
    }
    
    public String getStockStatusClass() {
        if (stock == 0) return "danger";
        if (stock < 10) return "warning";
        return "success";
    }
}

// Service for product management
@Service
class ProductService {
    
    private final Map<Long, Product> products = new HashMap<>();
    private Long nextId = 1L;
    
    public ProductService() {
        initializeSampleData();
    }
    
    private void initializeSampleData() {
        createProduct(new Product("MacBook Pro 16\"", "High-performance laptop", 2499.99, 15, "Electronics"));
        createProduct(new Product("iPhone 14", "Latest smartphone", 999.99, 50, "Electronics"));
        createProduct(new Product("Office Chair", "Ergonomic desk chair", 299.99, 25, "Furniture"));
        createProduct(new Product("Standing Desk", "Adjustable height desk", 499.99, 10, "Furniture"));
        createProduct(new Product("Wireless Mouse", "Bluetooth wireless mouse", 79.99, 100, "Accessories"));
        
        // Mark some as featured
        products.get(1L).setFeatured(true);
        products.get(2L).setFeatured(true);
        products.get(1L).setImageUrl("/images/macbook.jpg");
        products.get(2L).setImageUrl("/images/iphone.jpg");
    }
    
    public List<Product> findAll() {
        return new ArrayList<>(products.values());
    }
    
    public List<Product> findFeatured() {
        return products.values().stream()
            .filter(Product::isFeatured)
            .collect(Collectors.toList());
    }
    
    public List<Product> findByCategory(String category) {
        return products.values().stream()
            .filter(p -> p.getCategory().equalsIgnoreCase(category))
            .collect(Collectors.toList());
    }
    
    public Product findById(Long id) {
        return products.get(id);
    }
    
    public Product createProduct(Product product) {
        product.setId(nextId++);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        products.put(product.getId(), product);
        return product;
    }
    
    public Product updateProduct(Product product) {
        product.setUpdatedAt(LocalDateTime.now());
        products.put(product.getId(), product);
        return product;
    }
    
    public void deleteProduct(Long id) {
        products.remove(id);
    }
    
    public List<String> getCategories() {
        return products.values().stream()
            .map(Product::getCategory)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }
    
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProducts", products.size());
        stats.put("totalValue", products.values().stream()
            .mapToDouble(p -> p.getPrice() * p.getStock())
            .sum());
        stats.put("outOfStock", products.values().stream()
            .filter(p -> p.getStock() == 0)
            .count());
        stats.put("lowStock", products.values().stream()
            .filter(p -> p.getStock() > 0 && p.getStock() < 10)
            .count());
        return stats;
    }
}

// Main controller for Thymeleaf pages
@Controller
class ProductViewController {
    
    private final ProductService productService;
    
    public ProductViewController(ProductService productService) {
        this.productService = productService;
    }
    
    @GetMapping("/products")
    public String listProducts(Model model,
                              @RequestParam(required = false) String category,
                              @RequestParam(defaultValue = "false") boolean featured) {
        
        List<Product> products;
        
        if (featured) {
            products = productService.findFeatured();
            model.addAttribute("pageTitle", "Featured Products");
        } else if (category != null && !category.isEmpty()) {
            products = productService.findByCategory(category);
            model.addAttribute("pageTitle", category + " Products");
        } else {
            products = productService.findAll();
            model.addAttribute("pageTitle", "All Products");
        }
        
        model.addAttribute("products", products);
        model.addAttribute("categories", productService.getCategories());
        model.addAttribute("selectedCategory", category);
        model.addAttribute("showFeatured", featured);
        model.addAttribute("totalProducts", products.size());
        model.addAttribute("currentTime", LocalDateTime.now());
        
        return "products/list";
    }
    
    @GetMapping("/products/form")
    public String showProductForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", productService.getCategories());
        model.addAttribute("pageTitle", "Add New Product");
        return "products/form";
    }
    
    @PostMapping("/products/form")
    public String saveProduct(@Valid @ModelAttribute Product product,
                             BindingResult result,
                             Model model) {
        
        if (result.hasErrors()) {
            model.addAttribute("categories", productService.getCategories());
            model.addAttribute("pageTitle", "Add New Product");
            return "products/form";
        }
        
        productService.createProduct(product);
        model.addAttribute("successMessage", "Product created successfully!");
        
        return "redirect:/products";
    }
    
    @GetMapping("/products/{id}/edit")
    public String editProduct(@PathVariable Long id, Model model) {
        Product product = productService.findById(id);
        if (product == null) {
            return "redirect:/products";
        }
        
        model.addAttribute("product", product);
        model.addAttribute("categories", productService.getCategories());
        model.addAttribute("pageTitle", "Edit Product");
        return "products/form";
    }
    
    @PostMapping("/products/{id}/edit")
    public String updateProduct(@PathVariable Long id,
                               @Valid @ModelAttribute Product product,
                               BindingResult result,
                               Model model) {
        
        if (result.hasErrors()) {
            model.addAttribute("categories", productService.getCategories());
            model.addAttribute("pageTitle", "Edit Product");
            return "products/form";
        }
        
        product.setId(id);
        productService.updateProduct(product);
        model.addAttribute("successMessage", "Product updated successfully!");
        
        return "redirect:/products";
    }
    
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("statistics", productService.getStatistics());
        model.addAttribute("recentProducts", productService.findAll().stream()
            .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
            .limit(5)
            .collect(Collectors.toList()));
        model.addAttribute("featuredProducts", productService.findFeatured());
        model.addAttribute("pageTitle", "Dashboard");
        
        return "dashboard";
    }
    
    @GetMapping("/internationalization")
    public String i18nDemo(Model model, Locale locale) {
        model.addAttribute("currentLocale", locale);
        model.addAttribute("pageTitle", "Internationalization Demo");
        model.addAttribute("availableLocales", Arrays.asList(
            new Locale("en", "US"),
            new Locale("es", "ES"),
            new Locale("fr", "FR"),
            new Locale("de", "DE")
        ));
        
        return "i18n/demo";
    }
}

// Internationalization configuration
@Configuration
class InternationalizationConfig implements WebMvcConfigurer {
    
    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver resolver = new SessionLocaleResolver();
        resolver.setDefaultLocale(Locale.ENGLISH);
        return resolver;
    }
    
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}

// Utility class for Thymeleaf templates
@Component("templateUtils")
class TemplateUtils {
    
    public String formatCurrency(Double amount) {
        if (amount == null) return "$0.00";
        return String.format("$%.2f", amount);
    }
    
    public String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy"));
    }
    
    public String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
    }
    
    public String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
    
    public String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }
    
    public boolean isRecent(LocalDateTime dateTime) {
        if (dateTime == null) return false;
        return dateTime.isAfter(LocalDateTime.now().minusDays(7));
    }
}
```

---

## 🔄 Part 2: AJAX Integration (45 minutes)

### **Modern AJAX with Fetch API**

Create `AjaxIntegrationDemo.java`:

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * AJAX integration demonstration
 * Shows modern JavaScript integration with Spring Boot
 */
@SpringBootApplication
public class AjaxIntegrationDemo {
    
    public static void main(String[] args) {
        System.out.println("=== AJAX Integration Demo ===\n");
        SpringApplication.run(AjaxIntegrationDemo.class, args);
        
        System.out.println("🔄 AJAX Features Demonstrated:");
        System.out.println("   - Fetch API integration");
        System.out.println("   - Real-time search");
        System.out.println("   - Dynamic content loading");
        System.out.println("   - Form submission without page refresh");
        System.out.println("   - Progress indicators");
        System.out.println("   - Error handling");
        System.out.println();
        
        System.out.println("🌐 Interactive Pages:");
        System.out.println("   http://localhost:8080/ajax-demo");
        System.out.println("   http://localhost:8080/live-search");
        System.out.println("   http://localhost:8080/dynamic-content");
        System.out.println();
    }
}

// Task model for AJAX demo
class Task {
    private Long id;
    private String title;
    private String description;
    private String priority;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime dueDate;
    private boolean completed;
    
    public Task() {
        this.createdAt = LocalDateTime.now();
        this.status = "PENDING";
        this.completed = false;
    }
    
    public Task(String title, String description, String priority) {
        this();
        this.title = title;
        this.description = description;
        this.priority = priority;
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}

// Service for task management
@Service
class TaskService {
    
    private final Map<Long, Task> tasks = new ConcurrentHashMap<>();
    private Long nextId = 1L;
    
    public TaskService() {
        initializeSampleData();
    }
    
    private void initializeSampleData() {
        createTask(new Task("Complete project documentation", "Write comprehensive documentation for the project", "HIGH"));
        createTask(new Task("Fix login bug", "Resolve issue with user authentication", "HIGH"));
        createTask(new Task("Update dependencies", "Update all project dependencies to latest versions", "MEDIUM"));
        createTask(new Task("Code review", "Review pull request from team member", "MEDIUM"));
        createTask(new Task("Backup database", "Create backup of production database", "LOW"));
        
        // Mark some as completed
        tasks.get(5L).setCompleted(true);
        tasks.get(5L).setStatus("COMPLETED");
    }
    
    public List<Task> findAll() {
        return new ArrayList<>(tasks.values());
    }
    
    public List<Task> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return findAll();
        }
        
        String searchQuery = query.toLowerCase();
        return tasks.values().stream()
            .filter(task -> 
                task.getTitle().toLowerCase().contains(searchQuery) ||
                task.getDescription().toLowerCase().contains(searchQuery) ||
                task.getPriority().toLowerCase().contains(searchQuery) ||
                task.getStatus().toLowerCase().contains(searchQuery)
            )
            .collect(Collectors.toList());
    }
    
    public List<Task> findByStatus(String status) {
        return tasks.values().stream()
            .filter(task -> task.getStatus().equalsIgnoreCase(status))
            .collect(Collectors.toList());
    }
    
    public List<Task> findByPriority(String priority) {
        return tasks.values().stream()
            .filter(task -> task.getPriority().equalsIgnoreCase(priority))
            .collect(Collectors.toList());
    }
    
    public Task findById(Long id) {
        return tasks.get(id);
    }
    
    public Task createTask(Task task) {
        task.setId(nextId++);
        task.setCreatedAt(LocalDateTime.now());
        tasks.put(task.getId(), task);
        return task;
    }
    
    public Task updateTask(Task task) {
        tasks.put(task.getId(), task);
        return task;
    }
    
    public void deleteTask(Long id) {
        tasks.remove(id);
    }
    
    public Task toggleComplete(Long id) {
        Task task = tasks.get(id);
        if (task != null) {
            task.setCompleted(!task.isCompleted());
            task.setStatus(task.isCompleted() ? "COMPLETED" : "PENDING");
        }
        return task;
    }
    
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", tasks.size());
        stats.put("completed", tasks.values().stream().filter(Task::isCompleted).count());
        stats.put("pending", tasks.values().stream().filter(task -> !task.isCompleted()).count());
        stats.put("highPriority", tasks.values().stream().filter(task -> "HIGH".equals(task.getPriority())).count());
        return stats;
    }
}

// REST Controller for AJAX endpoints
@RestController
@RequestMapping("/api/tasks")
class TaskRestController {
    
    private final TaskService taskService;
    
    public TaskRestController(TaskService taskService) {
        this.taskService = taskService;
    }
    
    @GetMapping
    public List<Task> getAllTasks() {
        return taskService.findAll();
    }
    
    @GetMapping("/search")
    public List<Task> searchTasks(@RequestParam String query) {
        // Simulate network delay for demo
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return taskService.search(query);
    }
    
    @GetMapping("/filter")
    public List<Task> filterTasks(@RequestParam(required = false) String status,
                                 @RequestParam(required = false) String priority) {
        
        List<Task> tasks = taskService.findAll();
        
        if (status != null && !status.isEmpty()) {
            tasks = taskService.findByStatus(status);
        }
        
        if (priority != null && !priority.isEmpty()) {
            tasks = tasks.stream()
                .filter(task -> task.getPriority().equalsIgnoreCase(priority))
                .collect(Collectors.toList());
        }
        
        return tasks;
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Task> getTask(@PathVariable Long id) {
        Task task = taskService.findById(id);
        if (task != null) {
            return ResponseEntity.ok(task);
        }
        return ResponseEntity.notFound().build();
    }
    
    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody Task task) {
        // Simulate processing time
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Task createdTask = taskService.createTask(task);
        return ResponseEntity.ok(createdTask);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable Long id, @RequestBody Task task) {
        if (taskService.findById(id) == null) {
            return ResponseEntity.notFound().build();
        }
        
        task.setId(id);
        Task updatedTask = taskService.updateTask(task);
        return ResponseEntity.ok(updatedTask);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        if (taskService.findById(id) == null) {
            return ResponseEntity.notFound().build();
        }
        
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
    
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Task> toggleTaskComplete(@PathVariable Long id) {
        Task task = taskService.toggleComplete(id);
        if (task != null) {
            return ResponseEntity.ok(task);
        }
        return ResponseEntity.notFound().build();
    }
    
    @GetMapping("/statistics")
    public Map<String, Object> getStatistics() {
        return taskService.getStatistics();
    }
}

// View controller for AJAX demo pages
@Controller
class AjaxViewController {
    
    @GetMapping("/ajax-demo")
    public String ajaxDemo(Model model) {
        model.addAttribute("pageTitle", "AJAX Demo");
        return "ajax/demo";
    }
    
    @GetMapping("/live-search")
    public String liveSearch(Model model) {
        model.addAttribute("pageTitle", "Live Search");
        return "ajax/live-search";
    }
    
    @GetMapping("/dynamic-content")
    public String dynamicContent(Model model) {
        model.addAttribute("pageTitle", "Dynamic Content");
        return "ajax/dynamic-content";
    }
}

// Notification service for real-time updates
@Service
class NotificationService {
    
    private final List<String> notifications = new ArrayList<>();
    
    public void addNotification(String message) {
        notifications.add(LocalDateTime.now() + ": " + message);
        
        // Keep only last 10 notifications
        if (notifications.size() > 10) {
            notifications.remove(0);
        }
    }
    
    public List<String> getNotifications() {
        return new ArrayList<>(notifications);
    }
    
    public void clearNotifications() {
        notifications.clear();
    }
}

// Notification controller for real-time features
@RestController
@RequestMapping("/api/notifications")
class NotificationController {
    
    private final NotificationService notificationService;
    
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    @GetMapping
    public List<String> getNotifications() {
        return notificationService.getNotifications();
    }
    
    @PostMapping
    public ResponseEntity<Void> addNotification(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message != null && !message.trim().isEmpty()) {
            notificationService.addNotification(message);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }
    
    @DeleteMapping
    public ResponseEntity<Void> clearNotifications() {
        notificationService.clearNotifications();
        return ResponseEntity.ok().build();
    }
}
```

---

## 📝 Part 3: Form Handling & Validation (30 minutes)

### **Advanced Form Processing**

Create `FormValidationDemo.java`:

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Form handling and validation demonstration
 * Shows advanced form processing with Spring Boot and Thymeleaf
 */
@SpringBootApplication
public class FormValidationDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Form Validation Demo ===\n");
        SpringApplication.run(FormValidationDemo.class, args);
        
        System.out.println("📝 Form Features Demonstrated:");
        System.out.println("   - Server-side validation");
        System.out.println("   - Client-side validation");
        System.out.println("   - Custom validation messages");
        System.out.println("   - File upload handling");
        System.out.println("   - Multi-step forms");
        System.out.println("   - Form security (CSRF)");
        System.out.println();
        
        System.out.println("🌐 Form Pages:");
        System.out.println("   http://localhost:8080/forms/user-registration");
        System.out.println("   http://localhost:8080/forms/contact");
        System.out.println("   http://localhost:8080/forms/survey");
        System.out.println();
    }
}

// User registration form model
class UserRegistrationForm {
    
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
    private String username;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$", 
             message = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character")
    private String password;
    
    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;
    
    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name cannot exceed 50 characters")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name cannot exceed 50 characters")
    private String lastName;
    
    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;
    
    @NotBlank(message = "Please select a gender")
    private String gender;
    
    @Size(max = 15, message = "Phone number cannot exceed 15 characters")
    @Pattern(regexp = "^\\+?[0-9\\s\\-\\(\\)]+$", message = "Please provide a valid phone number")
    private String phoneNumber;
    
    @NotBlank(message = "Country is required")
    private String country;
    
    @NotBlank(message = "City is required")
    private String city;
    
    @Size(max = 200, message = "Address cannot exceed 200 characters")
    private String address;
    
    @NotEmpty(message = "Please select at least one interest")
    private List<String> interests;
    
    @AssertTrue(message = "You must agree to the terms and conditions")
    private boolean agreeToTerms;
    
    private boolean subscribeToNewsletter;
    
    // Constructors
    public UserRegistrationForm() {
        this.interests = new ArrayList<>();
    }
    
    // Getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public List<String> getInterests() { return interests; }
    public void setInterests(List<String> interests) { this.interests = interests; }
    
    public boolean isAgreeToTerms() { return agreeToTerms; }
    public void setAgreeToTerms(boolean agreeToTerms) { this.agreeToTerms = agreeToTerms; }
    
    public boolean isSubscribeToNewsletter() { return subscribeToNewsletter; }
    public void setSubscribeToNewsletter(boolean subscribeToNewsletter) { this.subscribeToNewsletter = subscribeToNewsletter; }
    
    // Custom validation methods
    public boolean isPasswordMatching() {
        return password != null && password.equals(confirmPassword);
    }
    
    public int getAge() {
        if (dateOfBirth != null) {
            return LocalDate.now().getYear() - dateOfBirth.getYear();
        }
        return 0;
    }
}

// Contact form model
class ContactForm {
    
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;
    
    @NotBlank(message = "Subject is required")
    @Size(max = 200, message = "Subject cannot exceed 200 characters")
    private String subject;
    
    @NotBlank(message = "Message is required")
    @Size(min = 10, max = 1000, message = "Message must be between 10 and 1000 characters")
    private String message;
    
    @NotBlank(message = "Please select a category")
    private String category;
    
    @NotBlank(message = "Please select priority")
    private String priority;
    
    private boolean sendCopy;
    
    // Constructors
    public ContactForm() {}
    
    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    
    public boolean isSendCopy() { return sendCopy; }
    public void setSendCopy(boolean sendCopy) { this.sendCopy = sendCopy; }
}

// Form controller
@Controller
@RequestMapping("/forms")
class FormController {
    
    @GetMapping("/user-registration")
    public String showUserRegistrationForm(Model model) {
        model.addAttribute("userForm", new UserRegistrationForm());
        model.addAttribute("pageTitle", "User Registration");
        
        // Add form data
        model.addAttribute("genders", Arrays.asList("MALE", "FEMALE", "OTHER"));
        model.addAttribute("countries", Arrays.asList("USA", "Canada", "UK", "Germany", "France", "Spain"));
        model.addAttribute("interests", Arrays.asList("Technology", "Sports", "Music", "Reading", "Travel", "Cooking", "Gaming"));
        
        return "forms/user-registration";
    }
    
    @PostMapping("/user-registration")
    public String processUserRegistration(@Valid @ModelAttribute("userForm") UserRegistrationForm form,
                                         BindingResult result,
                                         Model model,
                                         RedirectAttributes redirectAttributes) {
        
        // Custom validation
        if (!form.isPasswordMatching()) {
            result.addError(new FieldError("userForm", "confirmPassword", "Passwords do not match"));
        }
        
        if (form.getAge() < 18) {
            result.addError(new FieldError("userForm", "dateOfBirth", "You must be at least 18 years old"));
        }
        
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "User Registration");
            model.addAttribute("genders", Arrays.asList("MALE", "FEMALE", "OTHER"));
            model.addAttribute("countries", Arrays.asList("USA", "Canada", "UK", "Germany", "France", "Spain"));
            model.addAttribute("interests", Arrays.asList("Technology", "Sports", "Music", "Reading", "Travel", "Cooking", "Gaming"));
            return "forms/user-registration";
        }
        
        // Process successful registration
        System.out.println("User registered successfully: " + form.getUsername());
        redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Welcome, " + form.getFirstName() + "!");
        
        return "redirect:/forms/user-registration";
    }
    
    @GetMapping("/contact")
    public String showContactForm(Model model) {
        model.addAttribute("contactForm", new ContactForm());
        model.addAttribute("pageTitle", "Contact Us");
        
        // Add form data
        model.addAttribute("categories", Arrays.asList("General Inquiry", "Technical Support", "Sales", "Feedback", "Complaint"));
        model.addAttribute("priorities", Arrays.asList("LOW", "MEDIUM", "HIGH", "URGENT"));
        
        return "forms/contact";
    }
    
    @PostMapping("/contact")
    public String processContactForm(@Valid @ModelAttribute("contactForm") ContactForm form,
                                   BindingResult result,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Contact Us");
            model.addAttribute("categories", Arrays.asList("General Inquiry", "Technical Support", "Sales", "Feedback", "Complaint"));
            model.addAttribute("priorities", Arrays.asList("LOW", "MEDIUM", "HIGH", "URGENT"));
            return "forms/contact";
        }
        
        // Process successful submission
        System.out.println("Contact form submitted by: " + form.getName());
        redirectAttributes.addFlashAttribute("successMessage", "Thank you for your message! We'll get back to you soon.");
        
        return "redirect:/forms/contact";
    }
    
    @GetMapping("/survey")
    public String showSurveyForm(Model model) {
        model.addAttribute("pageTitle", "Customer Survey");
        return "forms/survey";
    }
    
    @PostMapping("/survey")
    public String processSurveyForm(@RequestParam Map<String, String> formData,
                                   RedirectAttributes redirectAttributes) {
        
        // Process survey data
        System.out.println("Survey submitted with data: " + formData);
        redirectAttributes.addFlashAttribute("successMessage", "Thank you for participating in our survey!");
        
        return "redirect:/forms/survey";
    }
}

// AJAX form controller
@RestController
@RequestMapping("/api/forms")
class AjaxFormController {
    
    @PostMapping("/validate-username")
    public Map<String, Object> validateUsername(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        Map<String, Object> response = new HashMap<>();
        
        // Simulate username validation
        if (username == null || username.trim().isEmpty()) {
            response.put("valid", false);
            response.put("message", "Username is required");
        } else if (username.length() < 3) {
            response.put("valid", false);
            response.put("message", "Username must be at least 3 characters long");
        } else if (username.equals("admin") || username.equals("root")) {
            response.put("valid", false);
            response.put("message", "Username is not available");
        } else {
            response.put("valid", true);
            response.put("message", "Username is available");
        }
        
        return response;
    }
    
    @PostMapping("/validate-email")
    public Map<String, Object> validateEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        Map<String, Object> response = new HashMap<>();
        
        // Simulate email validation
        if (email == null || email.trim().isEmpty()) {
            response.put("valid", false);
            response.put("message", "Email is required");
        } else if (!email.contains("@")) {
            response.put("valid", false);
            response.put("message", "Please provide a valid email address");
        } else if (email.equals("admin@example.com")) {
            response.put("valid", false);
            response.put("message", "Email is already registered");
        } else {
            response.put("valid", true);
            response.put("message", "Email is available");
        }
        
        return response;
    }
    
    @PostMapping("/submit-async")
    public Map<String, Object> submitFormAsync(@RequestBody Map<String, Object> formData) {
        // Simulate form processing
        try {
            Thread.sleep(2000); // Simulate processing time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Form submitted successfully!");
        response.put("timestamp", LocalDateTime.now());
        
        return response;
    }
}
```

---

## 🚀 Part 4: WebSocket Real-Time Communication (30 minutes)

### **WebSocket Integration**

Create `WebSocketConfigDemo.java`:

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.ui.Model;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket configuration demonstration
 * Shows real-time communication features
 */
@SpringBootApplication
@EnableScheduling
public class WebSocketConfigDemo {
    
    public static void main(String[] args) {
        System.out.println("=== WebSocket Configuration Demo ===\n");
        SpringApplication.run(WebSocketConfigDemo.class, args);
        
        System.out.println("🔌 WebSocket Features:");
        System.out.println("   - Real-time chat");
        System.out.println("   - Live notifications");
        System.out.println("   - System monitoring");
        System.out.println("   - Collaborative editing");
        System.out.println("   - Progress tracking");
        System.out.println();
        
        System.out.println("🌐 WebSocket Pages:");
        System.out.println("   http://localhost:8080/chat");
        System.out.println("   http://localhost:8080/notifications");
        System.out.println("   http://localhost:8080/monitoring");
        System.out.println();
    }
}

// WebSocket configuration
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/websocket").withSockJS();
    }
}

// Chat message model
class ChatMessage {
    private String content;
    private String sender;
    private String type;
    private LocalDateTime timestamp;
    
    public ChatMessage() {
        this.timestamp = LocalDateTime.now();
    }
    
    public ChatMessage(String content, String sender, String type) {
        this();
        this.content = content;
        this.sender = sender;
        this.type = type;
    }
    
    // Getters and setters
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getFormattedTimestamp() {
        return timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}

// System notification model
class SystemNotification {
    private String message;
    private String type;
    private LocalDateTime timestamp;
    private String level;
    
    public SystemNotification() {
        this.timestamp = LocalDateTime.now();
    }
    
    public SystemNotification(String message, String type, String level) {
        this();
        this.message = message;
        this.type = type;
        this.level = level;
    }
    
    // Getters and setters
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    
    public String getFormattedTimestamp() {
        return timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}

// System monitoring data
class SystemMetrics {
    private double cpuUsage;
    private double memoryUsage;
    private int activeUsers;
    private long totalRequests;
    private LocalDateTime timestamp;
    
    public SystemMetrics() {
        this.timestamp = LocalDateTime.now();
    }
    
    public SystemMetrics(double cpuUsage, double memoryUsage, int activeUsers, long totalRequests) {
        this();
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
        this.activeUsers = activeUsers;
        this.totalRequests = totalRequests;
    }
    
    // Getters and setters
    public double getCpuUsage() { return cpuUsage; }
    public void setCpuUsage(double cpuUsage) { this.cpuUsage = cpuUsage; }
    
    public double getMemoryUsage() { return memoryUsage; }
    public void setMemoryUsage(double memoryUsage) { this.memoryUsage = memoryUsage; }
    
    public int getActiveUsers() { return activeUsers; }
    public void setActiveUsers(int activeUsers) { this.activeUsers = activeUsers; }
    
    public long getTotalRequests() { return totalRequests; }
    public void setTotalRequests(long totalRequests) { this.totalRequests = totalRequests; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getFormattedTimestamp() {
        return timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}

// WebSocket controller for real-time features
@Controller
class WebSocketController {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final Set<String> activeUsers = ConcurrentHashMap.newKeySet();
    private long totalRequests = 0;
    
    public WebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }
    
    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(ChatMessage chatMessage) {
        return chatMessage;
    }
    
    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(ChatMessage chatMessage) {
        activeUsers.add(chatMessage.getSender());
        chatMessage.setType("JOIN");
        chatMessage.setContent(chatMessage.getSender() + " joined the chat!");
        return chatMessage;
    }
    
    @MessageMapping("/chat.removeUser")
    @SendTo("/topic/public")
    public ChatMessage removeUser(ChatMessage chatMessage) {
        activeUsers.remove(chatMessage.getSender());
        chatMessage.setType("LEAVE");
        chatMessage.setContent(chatMessage.getSender() + " left the chat!");
        return chatMessage;
    }
    
    @MessageMapping("/notification.send")
    @SendTo("/topic/notifications")
    public SystemNotification sendNotification(SystemNotification notification) {
        return notification;
    }
    
    // Scheduled task for system monitoring
    @Scheduled(fixedRate = 5000) // Every 5 seconds
    public void sendSystemMetrics() {
        // Simulate system metrics
        Random random = new Random();
        SystemMetrics metrics = new SystemMetrics(
            random.nextDouble() * 100,
            random.nextDouble() * 100,
            activeUsers.size(),
            totalRequests++
        );
        
        messagingTemplate.convertAndSend("/topic/system-metrics", metrics);
    }
    
    // Scheduled task for system notifications
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void sendSystemNotifications() {
        String[] notificationMessages = {
            "System backup completed successfully",
            "New user registered",
            "Database optimization finished",
            "Security scan completed",
            "System update available"
        };
        
        Random random = new Random();
        String message = notificationMessages[random.nextInt(notificationMessages.length)];
        
        SystemNotification notification = new SystemNotification(
            message,
            "SYSTEM",
            "INFO"
        );
        
        messagingTemplate.convertAndSend("/topic/notifications", notification);
    }
    
    // Progress tracking for long-running operations
    @PostMapping("/api/start-operation")
    @ResponseBody
    public Map<String, Object> startLongRunningOperation() {
        new Thread(() -> {
            for (int i = 0; i <= 100; i += 10) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                
                Map<String, Object> progress = new HashMap<>();
                progress.put("percentage", i);
                progress.put("message", "Processing... " + i + "%");
                progress.put("timestamp", LocalDateTime.now());
                
                messagingTemplate.convertAndSend("/topic/progress", progress);
            }
        }).start();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Operation started");
        response.put("success", true);
        return response;
    }
    
    @GetMapping("/chat")
    public String chat(Model model) {
        model.addAttribute("pageTitle", "Real-time Chat");
        return "websocket/chat";
    }
    
    @GetMapping("/notifications")
    public String notifications(Model model) {
        model.addAttribute("pageTitle", "Live Notifications");
        return "websocket/notifications";
    }
    
    @GetMapping("/monitoring")
    public String monitoring(Model model) {
        model.addAttribute("pageTitle", "System Monitoring");
        return "websocket/monitoring";
    }
}
```

---

## 📋 Practice Exercises

### Exercise 1: Interactive Dashboard
Create an interactive dashboard with:
- Real-time data updates via WebSocket
- AJAX-powered charts and graphs
- Dynamic filtering and searching
- Live user activity feed
- Responsive design with Bootstrap

### Exercise 2: Multi-step Form Wizard
Build a multi-step form wizard featuring:
- Step-by-step navigation
- Form validation at each step
- Progress indication
- Data persistence between steps
- Final review and submission

### Exercise 3: Real-time Collaboration Tool
Develop a collaborative tool with:
- Real-time document editing
- User presence indicators
- Chat integration
- Version history
- Conflict resolution

---

## 🎯 Key Takeaways

### Thymeleaf Best Practices:
1. **Natural templates**: HTML that works in browsers
2. **Expression language**: Powerful templating syntax
3. **Form binding**: Seamless integration with Spring MVC
4. **Internationalization**: Built-in i18n support
5. **Security**: XSS protection and CSRF integration

### AJAX Integration:
1. **Fetch API**: Modern JavaScript for HTTP requests
2. **Progressive enhancement**: Graceful degradation
3. **Error handling**: Comprehensive error management
4. **Loading states**: User feedback during operations
5. **Content updates**: Dynamic DOM manipulation

### Form Handling:
1. **Validation**: Server-side and client-side validation
2. **Error display**: User-friendly error messages
3. **Data binding**: Automatic form-to-object mapping
4. **Security**: CSRF protection and input sanitization
5. **User experience**: Smooth form interactions

### WebSocket Communication:
1. **Real-time updates**: Instant data synchronization
2. **Bidirectional communication**: Full-duplex communication
3. **Event-driven**: Message-based architecture
4. **Scalability**: Efficient real-time features
5. **Fallbacks**: SockJS for compatibility

---

## 🚀 Next Steps

Tomorrow, we'll explore **Application Properties & Profiles** (Day 26), where you'll learn:
- Configuration management strategies
- Environment-specific profiles
- Externalized configuration
- Property validation and binding
- Configuration security

Your frontend integration skills will be essential for creating modern, interactive web applications! 🌟