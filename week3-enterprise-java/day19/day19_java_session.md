# Day 19: Spring MVC & Web Applications (July 28, 2025)

**Date:** July 28, 2025  
**Duration:** 2.5 hours  
**Focus:** Master Spring MVC architecture, web controllers, templates, forms, and full-stack web development

---

## 🎯 Today's Learning Goals

By the end of this session, you'll:
- ✅ Understand Spring MVC architecture and request flow
- ✅ Master web controllers and request mapping patterns
- ✅ Work with Thymeleaf templates and view rendering
- ✅ Handle forms, validation, and data binding
- ✅ Implement session management and security basics
- ✅ Build a complete web application with CRUD operations

---

## 🌐 Part 1: Spring MVC Architecture (45 minutes)

### **Understanding Spring MVC**

**Spring MVC** is a web framework built on the Model-View-Controller pattern that provides:
- **DispatcherServlet**: Front controller that handles all requests
- **Controllers**: Handle requests and return model data
- **Views**: Render the response (HTML, JSON, etc.)
- **Model**: Data passed between controller and view

**Request Flow:**
1. Client sends request to DispatcherServlet
2. DispatcherServlet consults HandlerMapping
3. Controller processes request and returns ModelAndView
4. ViewResolver resolves view name to actual view
5. View renders response with model data

### **Spring MVC Fundamentals Demo**

Create `SpringMvcFundamentalsDemo.java`:

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Spring MVC fundamentals demonstration
 * Shows controller patterns, view resolution, and request handling
 */
@SpringBootApplication
public class SpringMvcFundamentalsDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Spring MVC Web Application Demo ===\n");
        
        SpringApplication app = new SpringApplication(SpringMvcFundamentalsDemo.class);
        
        // Configure application properties
        Properties props = new Properties();
        props.setProperty("spring.application.name", "spring-mvc-demo");
        props.setProperty("server.port", "8080");
        props.setProperty("spring.thymeleaf.cache", "false"); // Disable cache for development
        props.setProperty("spring.thymeleaf.prefix", "classpath:/templates/");
        props.setProperty("spring.thymeleaf.suffix", ".html");
        
        app.setDefaultProperties(props);
        app.run(args);
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        System.out.println("🚀 Spring MVC Web Application started successfully!");
        System.out.println("🌐 Application URL: http://localhost:8080");
        System.out.println("\n📋 Available Web Pages:");
        System.out.println("   GET  /                    - Home page");
        System.out.println("   GET  /about               - About page");
        System.out.println("   GET  /contact             - Contact page");
        System.out.println("   GET  /demo/params         - Request parameters demo");
        System.out.println("   GET  /demo/session        - Session management demo");
        System.out.println("   GET  /demo/model          - Model attributes demo");
        System.out.println("   GET  /demo/redirect       - Redirect demo");
        System.out.println("   GET  /users               - User management");
        System.out.println();
    }
}

// Main web controller
@Controller
public class HomeController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/")
    public String home(Model model, HttpSession session) {
        model.addAttribute("title", "Welcome to Spring MVC Demo");
        model.addAttribute("message", "Learn Spring MVC with hands-on examples");
        model.addAttribute("currentTime", LocalDateTime.now());
        
        // Track page views
        Integer pageViews = (Integer) session.getAttribute("pageViews");
        if (pageViews == null) {
            pageViews = 0;
        }
        pageViews++;
        session.setAttribute("pageViews", pageViews);
        model.addAttribute("pageViews", pageViews);
        
        return "home"; // Returns home.html template
    }
    
    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("title", "About Spring MVC");
        model.addAttribute("description", "Spring MVC is a powerful web framework for building web applications");
        
        List<String> features = Arrays.asList(
            "Model-View-Controller architecture",
            "Flexible request mapping",
            "Data binding and validation",
            "Template engine integration",
            "RESTful web services",
            "Interceptors and filters"
        );
        model.addAttribute("features", features);
        
        return "about";
    }
    
    @GetMapping("/contact")
    public String contact(Model model) {
        model.addAttribute("title", "Contact Us");
        model.addAttribute("contactForm", new ContactForm());
        return "contact";
    }
    
    @PostMapping("/contact")
    public String submitContact(@ModelAttribute ContactForm contactForm, 
                               RedirectAttributes redirectAttributes) {
        // Process contact form
        System.out.println("Contact form submitted:");
        System.out.println("  Name: " + contactForm.getName());
        System.out.println("  Email: " + contactForm.getEmail());
        System.out.println("  Message: " + contactForm.getMessage());
        
        redirectAttributes.addFlashAttribute("successMessage", 
            "Thank you, " + contactForm.getName() + "! Your message has been sent.");
        
        return "redirect:/contact";
    }
}

// Demo controller for various MVC features
@Controller
@RequestMapping("/demo")
public class DemoController {
    
    @GetMapping("/params")
    public String requestParams(@RequestParam(value = "name", defaultValue = "Guest") String name,
                               @RequestParam(value = "age", required = false) Integer age,
                               @RequestParam(value = "city", required = false) String city,
                               Model model) {
        
        model.addAttribute("title", "Request Parameters Demo");
        model.addAttribute("name", name);
        model.addAttribute("age", age);
        model.addAttribute("city", city);
        
        return "demo/params";
    }
    
    @GetMapping("/session")
    public String sessionDemo(HttpSession session, Model model) {
        // Store and retrieve session data
        String sessionId = session.getId();
        LocalDateTime creationTime = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(session.getCreationTime()), 
            java.time.ZoneId.systemDefault());
        
        // Increment counter
        Integer counter = (Integer) session.getAttribute("counter");
        if (counter == null) {
            counter = 0;
        }
        counter++;
        session.setAttribute("counter", counter);
        
        model.addAttribute("title", "Session Management Demo");
        model.addAttribute("sessionId", sessionId);
        model.addAttribute("creationTime", creationTime);
        model.addAttribute("counter", counter);
        
        return "demo/session";
    }
    
    @GetMapping("/model")
    public ModelAndView modelDemo() {
        ModelAndView mav = new ModelAndView("demo/model");
        
        mav.addObject("title", "Model Attributes Demo");
        mav.addObject("serverTime", LocalDateTime.now());
        
        // Add complex objects
        User user = new User("John Doe", "john@example.com", 30);
        mav.addObject("user", user);
        
        List<String> technologies = Arrays.asList("Spring", "Spring Boot", "Spring MVC", "Thymeleaf", "Bootstrap");
        mav.addObject("technologies", technologies);
        
        Map<String, String> configuration = new HashMap<>();
        configuration.put("Environment", "Development");
        configuration.put("Version", "1.0.0");
        configuration.put("Database", "H2");
        mav.addObject("configuration", configuration);
        
        return mav;
    }
    
    @GetMapping("/redirect")
    public String redirectDemo(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("message", "This message was passed via redirect!");
        redirectAttributes.addAttribute("source", "redirect-demo");
        return "redirect:/demo/redirect-target";
    }
    
    @GetMapping("/redirect-target")
    public String redirectTarget(@RequestParam(required = false) String source, Model model) {
        model.addAttribute("title", "Redirect Target");
        model.addAttribute("source", source);
        return "demo/redirect-target";
    }
}

// Path variable controller
@Controller
@RequestMapping("/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping
    public String listUsers(Model model) {
        List<User> users = userService.findAll();
        model.addAttribute("title", "User Management");
        model.addAttribute("users", users);
        model.addAttribute("newUser", new User());
        return "users/list";
    }
    
    @GetMapping("/{id}")
    public String viewUser(@PathVariable Long id, Model model) {
        User user = userService.findById(id);
        if (user == null) {
            return "redirect:/users?error=User not found";
        }
        
        model.addAttribute("title", "User Details");
        model.addAttribute("user", user);
        return "users/view";
    }
    
    @PostMapping
    public String createUser(@ModelAttribute User user, RedirectAttributes redirectAttributes) {
        User savedUser = userService.save(user);
        redirectAttributes.addFlashAttribute("successMessage", 
            "User '" + savedUser.getName() + "' created successfully!");
        return "redirect:/users";
    }
    
    @GetMapping("/{id}/edit")
    public String editUser(@PathVariable Long id, Model model) {
        User user = userService.findById(id);
        if (user == null) {
            return "redirect:/users?error=User not found";
        }
        
        model.addAttribute("title", "Edit User");
        model.addAttribute("user", user);
        return "users/edit";
    }
    
    @PostMapping("/{id}")
    public String updateUser(@PathVariable Long id, 
                            @ModelAttribute User user, 
                            RedirectAttributes redirectAttributes) {
        user.setId(id);
        User updatedUser = userService.save(user);
        redirectAttributes.addFlashAttribute("successMessage", 
            "User '" + updatedUser.getName() + "' updated successfully!");
        return "redirect:/users/" + id;
    }
    
    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = userService.findById(id);
        if (user != null) {
            userService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "User '" + user.getName() + "' deleted successfully!");
        }
        return "redirect:/users";
    }
}

// Error handling controller
@Controller
public class ErrorController {
    
    @GetMapping("/error")
    public String error(HttpServletRequest request, Model model) {
        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        String errorMessage = (String) request.getAttribute("javax.servlet.error.message");
        
        model.addAttribute("title", "Error");
        model.addAttribute("statusCode", statusCode);
        model.addAttribute("errorMessage", errorMessage);
        
        return "error";
    }
}

// Service layer
@org.springframework.stereotype.Service
class UserService {
    
    private final Map<Long, User> users = new HashMap<>();
    private final java.util.concurrent.atomic.AtomicLong idGenerator = 
        new java.util.concurrent.atomic.AtomicLong(1);
    
    public UserService() {
        // Initialize with sample data
        save(new User("John Doe", "john@example.com", 30));
        save(new User("Jane Smith", "jane@example.com", 25));
        save(new User("Bob Johnson", "bob@example.com", 35));
    }
    
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }
    
    public User findById(Long id) {
        return users.get(id);
    }
    
    public User save(User user) {
        if (user.getId() == null) {
            user.setId(idGenerator.getAndIncrement());
        }
        users.put(user.getId(), user);
        return user;
    }
    
    public void deleteById(Long id) {
        users.remove(id);
    }
}

// Domain models
class User {
    private Long id;
    private String name;
    private String email;
    private Integer age;
    
    public User() {}
    
    public User(String name, String email, Integer age) {
        this.name = name;
        this.email = email;
        this.age = age;
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
}

class ContactForm {
    private String name;
    private String email;
    private String message;
    
    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
```

---

## 🎨 Part 2: Thymeleaf Templates and Views (45 minutes)

### **Template Engine Integration**

Thymeleaf is a modern server-side Java template engine that integrates seamlessly with Spring MVC, providing natural templating capabilities.

### **Thymeleaf Templates**

Create template examples that would be stored in `src/main/resources/templates/`:

**`layout.html` (Base Layout Template):**
```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title th:text="${title} + ' - Spring MVC Demo'">Spring MVC Demo</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="/css/custom.css" rel="stylesheet" th:if="${@environment.acceptsProfiles('!production')}">
</head>
<body>
    <!-- Navigation -->
    <nav class="navbar navbar-expand-lg navbar-dark bg-primary">
        <div class="container">
            <a class="navbar-brand" href="/">Spring MVC Demo</a>
            <div class="navbar-nav">
                <a class="nav-link" href="/">Home</a>
                <a class="nav-link" href="/about">About</a>
                <a class="nav-link" href="/contact">Contact</a>
                <a class="nav-link" href="/users">Users</a>
                <div class="dropdown">
                    <a class="nav-link dropdown-toggle" href="#" role="button" data-bs-toggle="dropdown">
                        Demo
                    </a>
                    <ul class="dropdown-menu">
                        <li><a class="dropdown-item" href="/demo/params?name=John&age=25">Parameters</a></li>
                        <li><a class="dropdown-item" href="/demo/session">Session</a></li>
                        <li><a class="dropdown-item" href="/demo/model">Model</a></li>
                        <li><a class="dropdown-item" href="/demo/redirect">Redirect</a></li>
                    </ul>
                </div>
            </div>
        </div>
    </nav>

    <!-- Main Content -->
    <div class="container mt-4">
        <!-- Flash Messages -->
        <div th:if="${successMessage}" class="alert alert-success alert-dismissible fade show" role="alert">
            <span th:text="${successMessage}"></span>
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
        
        <div th:if="${errorMessage}" class="alert alert-danger alert-dismissible fade show" role="alert">
            <span th:text="${errorMessage}"></span>
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>

        <!-- Page Content -->
        <div layout:fragment="content">
            <!-- Content will be inserted here -->
        </div>
    </div>

    <!-- Footer -->
    <footer class="bg-light mt-5 py-4">
        <div class="container text-center">
            <p class="mb-0">&copy; 2025 Spring MVC Demo. Built with Spring Boot and Thymeleaf.</p>
            <p class="small text-muted">
                Page views this session: <span th:text="${session.pageViews ?: 0}">0</span>
            </p>
        </div>
    </footer>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
```

**`home.html` (Home Page Template):**
```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org" xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layout}">
<head>
    <title th:text="${title}">Home</title>
</head>
<body>
    <div layout:fragment="content">
        <div class="jumbotron bg-primary text-white p-5 rounded">
            <h1 class="display-4" th:text="${title}">Welcome to Spring MVC Demo</h1>
            <p class="lead" th:text="${message}">Learn Spring MVC with hands-on examples</p>
            <hr class="my-4">
            <p>Current server time: <span th:text="${#temporals.format(currentTime, 'yyyy-MM-dd HH:mm:ss')}"></span></p>
            <a class="btn btn-light btn-lg" href="/about" role="button">Learn More</a>
        </div>

        <div class="row mt-5">
            <div class="col-md-4">
                <div class="card">
                    <div class="card-body">
                        <h5 class="card-title">Controllers</h5>
                        <p class="card-text">Learn how to create web controllers with Spring MVC annotations.</p>
                        <a href="/demo/params" class="btn btn-primary">Try Demo</a>
                    </div>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card">
                    <div class="card-body">
                        <h5 class="card-title">Templates</h5>
                        <p class="card-text">Explore Thymeleaf templating with dynamic content rendering.</p>
                        <a href="/demo/model" class="btn btn-primary">Try Demo</a>
                    </div>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card">
                    <div class="card-body">
                        <h5 class="card-title">Forms</h5>
                        <p class="card-text">Handle form submissions and data binding with validation.</p>
                        <a href="/contact" class="btn btn-primary">Try Demo</a>
                    </div>
                </div>
            </div>
        </div>

        <!-- Session Information -->
        <div class="mt-5">
            <div class="card">
                <div class="card-header">
                    <h5 class="mb-0">Session Information</h5>
                </div>
                <div class="card-body">
                    <p><strong>Page Views:</strong> <span th:text="${pageViews}">0</span></p>
                    <p><strong>Current Time:</strong> <span th:text="${#temporals.format(currentTime, 'yyyy-MM-dd HH:mm:ss')}"></span></p>
                    <a href="/demo/session" class="btn btn-outline-primary">Session Demo</a>
                </div>
            </div>
        </div>
    </div>
</body>
</html>
```

### **Advanced Template Features Demo**

Create `ThymeleafAdvancedDemo.java`:

```java
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Advanced Thymeleaf features demonstration
 * Shows conditionals, loops, fragments, and utility methods
 */
@Controller
@RequestMapping("/advanced")
public class ThymeleafAdvancedDemo {
    
    @GetMapping("/conditionals")
    public String conditionals(Model model) {
        model.addAttribute("title", "Thymeleaf Conditionals");
        model.addAttribute("user", new User("John Doe", "john@example.com", 30));
        model.addAttribute("isLoggedIn", true);
        model.addAttribute("userRole", "ADMIN");
        model.addAttribute("notifications", Arrays.asList("New message", "System update"));
        
        return "advanced/conditionals";
    }
    
    @GetMapping("/loops")
    public String loops(Model model) {
        model.addAttribute("title", "Thymeleaf Loops");
        
        List<Product> products = Arrays.asList(
            new Product("Laptop", "Electronics", 999.99, 5),
            new Product("Book", "Education", 29.99, 0),
            new Product("Headphones", "Electronics", 199.99, 3),
            new Product("Desk", "Furniture", 299.99, 8)
        );
        model.addAttribute("products", products);
        
        Map<String, Integer> categoryCount = new HashMap<>();
        categoryCount.put("Electronics", 15);
        categoryCount.put("Education", 8);
        categoryCount.put("Furniture", 12);
        model.addAttribute("categoryCount", categoryCount);
        
        return "advanced/loops";
    }
    
    @GetMapping("/utilities")
    public String utilities(Model model) {
        model.addAttribute("title", "Thymeleaf Utilities");
        model.addAttribute("currentDate", LocalDateTime.now());
        model.addAttribute("text", "Hello, Thymeleaf World!");
        model.addAttribute("numbers", Arrays.asList(1, 2, 3, 4, 5));
        model.addAttribute("price", 1234.56);
        
        return "advanced/utilities";
    }
    
    @GetMapping("/fragments")
    public String fragments(Model model) {
        model.addAttribute("title", "Thymeleaf Fragments");
        model.addAttribute("cards", Arrays.asList(
            new Card("Spring Boot", "Rapid application development", "primary"),
            new Card("Spring MVC", "Web application framework", "success"),
            new Card("Thymeleaf", "Modern template engine", "info")
        ));
        
        return "advanced/fragments";
    }
}

// Additional models
class Product {
    private String name;
    private String category;
    private double price;
    private int stock;
    
    public Product(String name, String category, double price, int stock) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
    }
    
    // Getters
    public String getName() { return name; }
    public String getCategory() { return category; }
    public double getPrice() { return price; }
    public int getStock() { return stock; }
    public boolean isInStock() { return stock > 0; }
}

class Card {
    private String title;
    private String description;
    private String type;
    
    public Card(String title, String description, String type) {
        this.title = title;
        this.description = description;
        this.type = type;
    }
    
    // Getters
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getType() { return type; }
}
```

**Template Examples:**

**`advanced/conditionals.html`:**
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layout}">
<body>
    <div layout:fragment="content">
        <h2 th:text="${title}">Conditionals</h2>
        
        <!-- Simple conditional -->
        <div th:if="${isLoggedIn}" class="alert alert-success">
            <p>Welcome back, <span th:text="${user.name}">User</span>!</p>
        </div>
        
        <div th:unless="${isLoggedIn}" class="alert alert-warning">
            <p>Please log in to continue.</p>
        </div>
        
        <!-- Conditional with role check -->
        <div th:if="${userRole == 'ADMIN'}" class="alert alert-info">
            <p>You have administrator privileges.</p>
        </div>
        
        <!-- Conditional with expression -->
        <div th:if="${user.age >= 18}" class="alert alert-primary">
            <p>User is an adult (age: <span th:text="${user.age}">0</span>)</p>
        </div>
        
        <!-- Switch statement -->
        <div th:switch="${userRole}">
            <p th:case="'ADMIN'" class="text-danger">Administrator Access</p>
            <p th:case="'USER'" class="text-primary">Standard User Access</p>
            <p th:case="*" class="text-muted">Guest Access</p>
        </div>
        
        <!-- Conditional CSS classes -->
        <p th:class="${notifications.size() > 0} ? 'badge bg-warning' : 'badge bg-secondary'">
            Notifications: <span th:text="${notifications.size()}">0</span>
        </p>
    </div>
</body>
</html>
```

**`advanced/loops.html`:**
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layout}">
<body>
    <div layout:fragment="content">
        <h2 th:text="${title}">Loops</h2>
        
        <!-- Simple list iteration -->
        <h4>Products</h4>
        <div class="row">
            <div th:each="product : ${products}" class="col-md-6 mb-3">
                <div class="card">
                    <div class="card-body">
                        <h5 class="card-title" th:text="${product.name}">Product Name</h5>
                        <p class="card-text">
                            Category: <span th:text="${product.category}">Category</span><br>
                            Price: $<span th:text="${#numbers.formatDecimal(product.price, 1, 2)}">0.00</span><br>
                            Stock: <span th:text="${product.stock}">0</span>
                            <span th:if="${!product.inStock}" class="badge bg-danger ms-2">Out of Stock</span>
                        </p>
                    </div>
                </div>
            </div>
        </div>
        
        <!-- Table with iteration status -->
        <h4>Product Table</h4>
        <table class="table table-striped">
            <thead>
                <tr>
                    <th>#</th>
                    <th>Name</th>
                    <th>Category</th>
                    <th>Price</th>
                    <th>Status</th>
                </tr>
            </thead>
            <tbody>
                <tr th:each="product, iterStat : ${products}" 
                    th:class="${iterStat.odd} ? 'table-light' : ''">
                    <td th:text="${iterStat.count}">1</td>
                    <td th:text="${product.name}">Product</td>
                    <td th:text="${product.category}">Category</td>
                    <td th:text="'$' + ${#numbers.formatDecimal(product.price, 1, 2)}">Price</td>
                    <td>
                        <span th:if="${product.inStock}" class="badge bg-success">In Stock</span>
                        <span th:unless="${product.inStock}" class="badge bg-danger">Out of Stock</span>
                    </td>
                </tr>
            </tbody>
        </table>
        
        <!-- Map iteration -->
        <h4>Category Statistics</h4>
        <div class="row">
            <div th:each="entry : ${categoryCount}" class="col-md-4">
                <div class="card text-center">
                    <div class="card-body">
                        <h5 th:text="${entry.key}">Category</h5>
                        <p class="card-text display-6" th:text="${entry.value}">0</p>
                        <small class="text-muted">products</small>
                    </div>
                </div>
            </div>
        </div>
    </div>
</body>
</html>
```

---

## 📝 Part 3: Forms and Validation (45 minutes)

### **Form Handling and Data Binding**

Spring MVC provides powerful form handling capabilities with automatic data binding and validation.

### **Advanced Forms Demo**

Create `FormHandlingDemo.java`:

```java
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;

/**
 * Advanced form handling demonstration
 * Shows data binding, validation, and form processing
 */
@Controller
@RequestMapping("/forms")
public class FormHandlingDemo {
    
    @Autowired
    private EmployeeService employeeService;
    
    @GetMapping("/employee")
    public String employeeForm(Model model) {
        model.addAttribute("title", "Employee Registration");
        model.addAttribute("employee", new Employee());
        model.addAttribute("departments", getDepartments());
        model.addAttribute("skills", getAvailableSkills());
        return "forms/employee";
    }
    
    @PostMapping("/employee")
    public String submitEmployee(@Valid @ModelAttribute Employee employee, 
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("title", "Employee Registration");
            model.addAttribute("departments", getDepartments());
            model.addAttribute("skills", getAvailableSkills());
            return "forms/employee";
        }
        
        Employee savedEmployee = employeeService.save(employee);
        redirectAttributes.addFlashAttribute("successMessage", 
            "Employee " + savedEmployee.getFirstName() + " " + savedEmployee.getLastName() + 
            " registered successfully!");
        
        return "redirect:/forms/employees";
    }
    
    @GetMapping("/employees")
    public String listEmployees(Model model) {
        model.addAttribute("title", "Employee List");
        model.addAttribute("employees", employeeService.findAll());
        return "forms/employee-list";
    }
    
    @GetMapping("/employee/{id}/edit")
    public String editEmployee(@PathVariable Long id, Model model) {
        Employee employee = employeeService.findById(id);
        if (employee == null) {
            return "redirect:/forms/employees?error=Employee not found";
        }
        
        model.addAttribute("title", "Edit Employee");
        model.addAttribute("employee", employee);
        model.addAttribute("departments", getDepartments());
        model.addAttribute("skills", getAvailableSkills());
        return "forms/employee";
    }
    
    @GetMapping("/survey")
    public String surveyForm(Model model) {
        model.addAttribute("title", "Customer Survey");
        model.addAttribute("survey", new Survey());
        return "forms/survey";
    }
    
    @PostMapping("/survey")
    public String submitSurvey(@Valid @ModelAttribute Survey survey,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("title", "Customer Survey");
            return "forms/survey";
        }
        
        // Process survey
        System.out.println("Survey submitted: " + survey);
        
        redirectAttributes.addFlashAttribute("successMessage", 
            "Thank you for your feedback! Survey submitted successfully.");
        
        return "redirect:/forms/survey";
    }
    
    private List<String> getDepartments() {
        return Arrays.asList("Engineering", "Marketing", "Sales", "HR", "Finance");
    }
    
    private List<String> getAvailableSkills() {
        return Arrays.asList("Java", "Spring", "JavaScript", "Python", "React", "Angular", "Docker", "Kubernetes");
    }
}

// Employee service
@org.springframework.stereotype.Service
class EmployeeService {
    
    private final Map<Long, Employee> employees = new HashMap<>();
    private final java.util.concurrent.atomic.AtomicLong idGenerator = 
        new java.util.concurrent.atomic.AtomicLong(1);
    
    public List<Employee> findAll() {
        return new ArrayList<>(employees.values());
    }
    
    public Employee findById(Long id) {
        return employees.get(id);
    }
    
    public Employee save(Employee employee) {
        if (employee.getId() == null) {
            employee.setId(idGenerator.getAndIncrement());
        }
        employees.put(employee.getId(), employee);
        return employee;
    }
}

// Form models with validation
class Employee {
    private Long id;
    
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;
    
    @NotNull(message = "Birth date is required")
    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;
    
    @NotBlank(message = "Department is required")
    private String department;
    
    @NotNull(message = "Salary is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Salary must be positive")
    @DecimalMax(value = "1000000.0", message = "Salary cannot exceed 1,000,000")
    private Double salary;
    
    private List<String> skills = new ArrayList<>();
    
    private boolean active = true;
    
    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;
    
    // Constructors, getters, and setters
    public Employee() {}
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public Double getSalary() { return salary; }
    public void setSalary(Double salary) { this.salary = salary; }
    
    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
}

class Survey {
    @NotBlank(message = "Name is required")
    private String customerName;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email")
    private String email;
    
    @NotNull(message = "Please rate our service")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer serviceRating;
    
    @NotNull(message = "Please rate our product")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer productRating;
    
    @NotBlank(message = "Please select how you heard about us")
    private String hearAboutUs;
    
    private boolean wouldRecommend;
    
    private List<String> improvements = new ArrayList<>();
    
    @Size(max = 1000, message = "Comments cannot exceed 1000 characters")
    private String comments;
    
    // Getters and setters
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public Integer getServiceRating() { return serviceRating; }
    public void setServiceRating(Integer serviceRating) { this.serviceRating = serviceRating; }
    
    public Integer getProductRating() { return productRating; }
    public void setProductRating(Integer productRating) { this.productRating = productRating; }
    
    public String getHearAboutUs() { return hearAboutUs; }
    public void setHearAboutUs(String hearAboutUs) { this.hearAboutUs = hearAboutUs; }
    
    public boolean isWouldRecommend() { return wouldRecommend; }
    public void setWouldRecommend(boolean wouldRecommend) { this.wouldRecommend = wouldRecommend; }
    
    public List<String> getImprovements() { return improvements; }
    public void setImprovements(List<String> improvements) { this.improvements = improvements; }
    
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    
    @Override
    public String toString() {
        return "Survey{" +
                "customerName='" + customerName + '\'' +
                ", email='" + email + '\'' +
                ", serviceRating=" + serviceRating +
                ", productRating=" + productRating +
                ", hearAboutUs='" + hearAboutUs + '\'' +
                ", wouldRecommend=" + wouldRecommend +
                ", improvements=" + improvements +
                ", comments='" + comments + '\'' +
                '}';
    }
}
```

**Form Template Examples:**

**`forms/employee.html`:**
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layout}">
<body>
    <div layout:fragment="content">
        <h2 th:text="${title}">Employee Form</h2>
        
        <form th:action="@{/forms/employee}" th:object="${employee}" method="post" class="needs-validation" novalidate>
            <input type="hidden" th:field="*{id}">
            
            <div class="row">
                <div class="col-md-6">
                    <div class="mb-3">
                        <label for="firstName" class="form-label">First Name *</label>
                        <input type="text" class="form-control" th:field="*{firstName}" 
                               th:classappend="${#fields.hasErrors('firstName')} ? 'is-invalid' : ''">
                        <div class="invalid-feedback" th:if="${#fields.hasErrors('firstName')}" 
                             th:errors="*{firstName}">First name error</div>
                    </div>
                </div>
                <div class="col-md-6">
                    <div class="mb-3">
                        <label for="lastName" class="form-label">Last Name *</label>
                        <input type="text" class="form-control" th:field="*{lastName}"
                               th:classappend="${#fields.hasErrors('lastName')} ? 'is-invalid' : ''">
                        <div class="invalid-feedback" th:if="${#fields.hasErrors('lastName')}" 
                             th:errors="*{lastName}">Last name error</div>
                    </div>
                </div>
            </div>
            
            <div class="row">
                <div class="col-md-6">
                    <div class="mb-3">
                        <label for="email" class="form-label">Email *</label>
                        <input type="email" class="form-control" th:field="*{email}"
                               th:classappend="${#fields.hasErrors('email')} ? 'is-invalid' : ''">
                        <div class="invalid-feedback" th:if="${#fields.hasErrors('email')}" 
                             th:errors="*{email}">Email error</div>
                    </div>
                </div>
                <div class="col-md-6">
                    <div class="mb-3">
                        <label for="birthDate" class="form-label">Birth Date *</label>
                        <input type="date" class="form-control" th:field="*{birthDate}"
                               th:classappend="${#fields.hasErrors('birthDate')} ? 'is-invalid' : ''">
                        <div class="invalid-feedback" th:if="${#fields.hasErrors('birthDate')}" 
                             th:errors="*{birthDate}">Birth date error</div>
                    </div>
                </div>
            </div>
            
            <div class="row">
                <div class="col-md-6">
                    <div class="mb-3">
                        <label for="department" class="form-label">Department *</label>
                        <select class="form-select" th:field="*{department}"
                                th:classappend="${#fields.hasErrors('department')} ? 'is-invalid' : ''">
                            <option value="">Choose department...</option>
                            <option th:each="dept : ${departments}" th:value="${dept}" th:text="${dept}">Department</option>
                        </select>
                        <div class="invalid-feedback" th:if="${#fields.hasErrors('department')}" 
                             th:errors="*{department}">Department error</div>
                    </div>
                </div>
                <div class="col-md-6">
                    <div class="mb-3">
                        <label for="salary" class="form-label">Salary *</label>
                        <input type="number" class="form-control" th:field="*{salary}" step="0.01"
                               th:classappend="${#fields.hasErrors('salary')} ? 'is-invalid' : ''">
                        <div class="invalid-feedback" th:if="${#fields.hasErrors('salary')}" 
                             th:errors="*{salary}">Salary error</div>
                    </div>
                </div>
            </div>
            
            <div class="mb-3">
                <label class="form-label">Skills</label>
                <div class="row">
                    <div th:each="skill : ${skills}" class="col-md-3">
                        <div class="form-check">
                            <input class="form-check-input" type="checkbox" th:field="*{skills}" th:value="${skill}">
                            <label class="form-check-label" th:text="${skill}">Skill</label>
                        </div>
                    </div>
                </div>
            </div>
            
            <div class="mb-3">
                <div class="form-check">
                    <input class="form-check-input" type="checkbox" th:field="*{active}">
                    <label class="form-check-label">Active Employee</label>
                </div>
            </div>
            
            <div class="mb-3">
                <label for="notes" class="form-label">Notes</label>
                <textarea class="form-control" th:field="*{notes}" rows="3"
                          th:classappend="${#fields.hasErrors('notes')} ? 'is-invalid' : ''"></textarea>
                <div class="invalid-feedback" th:if="${#fields.hasErrors('notes')}" 
                     th:errors="*{notes}">Notes error</div>
            </div>
            
            <div class="mb-3">
                <button type="submit" class="btn btn-primary">Save Employee</button>
                <a href="/forms/employees" class="btn btn-secondary">Cancel</a>
            </div>
        </form>
    </div>
</body>
</html>
```

---

## 🛡️ Part 4: Complete Web Application (30 minutes)

### **Task Management Web Application**

Let's build a complete web application that demonstrates all Spring MVC concepts.

Create `TaskWebApplication.java`:

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Complete Task Management Web Application
 * Demonstrates full-stack Spring MVC development with CRUD operations
 */
@SpringBootApplication
public class TaskWebApplication {
    
    public static void main(String[] args) {
        System.out.println("=== Task Management Web Application ===\n");
        SpringApplication.run(TaskWebApplication.class, args);
    }
}

// Main task controller
@Controller
@RequestMapping("/tasks")
public class TaskWebController {
    
    @Autowired
    private TaskWebService taskService;
    
    @Autowired
    private UserWebService userService;
    
    @GetMapping
    public String listTasks(@RequestParam(required = false) String status,
                           @RequestParam(required = false) String priority,
                           @RequestParam(required = false) Long assigneeId,
                           Model model) {
        
        List<Task> tasks = taskService.findTasks(status, priority, assigneeId);
        
        model.addAttribute("title", "Task Management");
        model.addAttribute("tasks", tasks);
        model.addAttribute("users", userService.findAll());
        model.addAttribute("statuses", TaskStatus.values());
        model.addAttribute("priorities", TaskPriority.values());
        model.addAttribute("currentStatus", status);
        model.addAttribute("currentPriority", priority);
        model.addAttribute("currentAssigneeId", assigneeId);
        
        // Statistics
        Map<TaskStatus, Long> statusCounts = tasks.stream()
            .collect(Collectors.groupingBy(Task::getStatus, Collectors.counting()));
        model.addAttribute("statusCounts", statusCounts);
        
        return "tasks/list";
    }
    
    @GetMapping("/new")
    public String newTask(Model model) {
        model.addAttribute("title", "Create New Task");
        model.addAttribute("task", new Task());
        model.addAttribute("users", userService.findAll());
        model.addAttribute("priorities", TaskPriority.values());
        return "tasks/form";
    }
    
    @PostMapping
    public String createTask(@Valid @ModelAttribute Task task,
                            BindingResult bindingResult,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("title", "Create New Task");
            model.addAttribute("users", userService.findAll());
            model.addAttribute("priorities", TaskPriority.values());
            return "tasks/form";
        }
        
        Task savedTask = taskService.save(task);
        redirectAttributes.addFlashAttribute("successMessage", 
            "Task '" + savedTask.getTitle() + "' created successfully!");
        
        return "redirect:/tasks";
    }
    
    @GetMapping("/{id}")
    public String viewTask(@PathVariable Long id, Model model) {
        Task task = taskService.findById(id);
        if (task == null) {
            return "redirect:/tasks?error=Task not found";
        }
        
        model.addAttribute("title", "Task Details");
        model.addAttribute("task", task);
        model.addAttribute("assignee", userService.findById(task.getAssigneeId()));
        
        return "tasks/view";
    }
    
    @GetMapping("/{id}/edit")
    public String editTask(@PathVariable Long id, Model model) {
        Task task = taskService.findById(id);
        if (task == null) {
            return "redirect:/tasks?error=Task not found";
        }
        
        model.addAttribute("title", "Edit Task");
        model.addAttribute("task", task);
        model.addAttribute("users", userService.findAll());
        model.addAttribute("priorities", TaskPriority.values());
        model.addAttribute("statuses", TaskStatus.values());
        
        return "tasks/form";
    }
    
    @PostMapping("/{id}")
    public String updateTask(@PathVariable Long id,
                            @Valid @ModelAttribute Task task,
                            BindingResult bindingResult,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("title", "Edit Task");
            model.addAttribute("users", userService.findAll());
            model.addAttribute("priorities", TaskPriority.values());
            model.addAttribute("statuses", TaskStatus.values());
            return "tasks/form";
        }
        
        task.setId(id);
        task.setUpdatedAt(LocalDateTime.now());
        Task updatedTask = taskService.save(task);
        
        redirectAttributes.addFlashAttribute("successMessage", 
            "Task '" + updatedTask.getTitle() + "' updated successfully!");
        
        return "redirect:/tasks/" + id;
    }
    
    @PostMapping("/{id}/delete")
    public String deleteTask(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Task task = taskService.findById(id);
        if (task != null) {
            taskService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Task '" + task.getTitle() + "' deleted successfully!");
        }
        return "redirect:/tasks";
    }
    
    @PostMapping("/{id}/complete")
    public String completeTask(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Task task = taskService.findById(id);
        if (task != null) {
            task.setStatus(TaskStatus.COMPLETED);
            task.setCompletedAt(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());
            taskService.save(task);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Task '" + task.getTitle() + "' marked as completed!");
        }
        return "redirect:/tasks/" + id;
    }
}

// Dashboard controller
@Controller
public class DashboardController {
    
    @Autowired
    private TaskWebService taskService;
    
    @Autowired
    private UserWebService userService;
    
    @GetMapping("/")
    public String dashboard(Model model) {
        List<Task> allTasks = taskService.findAll();
        List<User> allUsers = userService.findAll();
        
        model.addAttribute("title", "Dashboard");
        model.addAttribute("totalTasks", allTasks.size());
        model.addAttribute("totalUsers", allUsers.size());
        
        // Task statistics
        Map<TaskStatus, Long> statusCounts = allTasks.stream()
            .collect(Collectors.groupingBy(Task::getStatus, Collectors.counting()));
        model.addAttribute("statusCounts", statusCounts);
        
        Map<TaskPriority, Long> priorityCounts = allTasks.stream()
            .collect(Collectors.groupingBy(Task::getPriority, Collectors.counting()));
        model.addAttribute("priorityCounts", priorityCounts);
        
        // Recent tasks
        List<Task> recentTasks = allTasks.stream()
            .sorted((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()))
            .limit(5)
            .collect(Collectors.toList());
        model.addAttribute("recentTasks", recentTasks);
        
        // Overdue tasks
        List<Task> overdueTasks = allTasks.stream()
            .filter(t -> t.getDueDate() != null && 
                        t.getDueDate().isBefore(LocalDateTime.now()) && 
                        t.getStatus() != TaskStatus.COMPLETED)
            .collect(Collectors.toList());
        model.addAttribute("overdueTasks", overdueTasks);
        
        return "dashboard";
    }
}

// Services
@org.springframework.stereotype.Service
class TaskWebService {
    
    private final Map<Long, Task> tasks = new HashMap<>();
    private final java.util.concurrent.atomic.AtomicLong idGenerator = 
        new java.util.concurrent.atomic.AtomicLong(1);
    
    public TaskWebService() {
        initializeSampleData();
    }
    
    private void initializeSampleData() {
        save(createSampleTask("Implement user authentication", "Add login/logout functionality", 
                             TaskPriority.HIGH, TaskStatus.IN_PROGRESS, 1L));
        save(createSampleTask("Design database schema", "Create ER diagram and tables", 
                             TaskPriority.MEDIUM, TaskStatus.COMPLETED, 2L));
        save(createSampleTask("Write unit tests", "Add test coverage for service layer", 
                             TaskPriority.LOW, TaskStatus.TODO, 1L));
        save(createSampleTask("Deploy to staging", "Set up staging environment", 
                             TaskPriority.HIGH, TaskStatus.TODO, 3L));
    }
    
    private Task createSampleTask(String title, String description, TaskPriority priority, 
                                 TaskStatus status, Long assigneeId) {
        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description);
        task.setPriority(priority);
        task.setStatus(status);
        task.setAssigneeId(assigneeId);
        task.setCreatedAt(LocalDateTime.now().minusDays((long) (Math.random() * 7)));
        task.setUpdatedAt(task.getCreatedAt());
        
        if (Math.random() > 0.5) {
            task.setDueDate(LocalDateTime.now().plusDays((long) (Math.random() * 14)));
        }
        
        return task;
    }
    
    public List<Task> findAll() {
        return new ArrayList<>(tasks.values());
    }
    
    public Task findById(Long id) {
        return tasks.get(id);
    }
    
    public Task save(Task task) {
        if (task.getId() == null) {
            task.setId(idGenerator.getAndIncrement());
            task.setCreatedAt(LocalDateTime.now());
        }
        task.setUpdatedAt(LocalDateTime.now());
        tasks.put(task.getId(), task);
        return task;
    }
    
    public void deleteById(Long id) {
        tasks.remove(id);
    }
    
    public List<Task> findTasks(String status, String priority, Long assigneeId) {
        return tasks.values().stream()
            .filter(t -> status == null || t.getStatus().name().equals(status))
            .filter(t -> priority == null || t.getPriority().name().equals(priority))
            .filter(t -> assigneeId == null || Objects.equals(t.getAssigneeId(), assigneeId))
            .sorted((t1, t2) -> t2.getUpdatedAt().compareTo(t1.getUpdatedAt()))
            .collect(Collectors.toList());
    }
}

@org.springframework.stereotype.Service
class UserWebService {
    
    private final Map<Long, User> users = new HashMap<>();
    
    public UserWebService() {
        users.put(1L, new User(1L, "John Doe", "john@example.com", 30));
        users.put(2L, new User(2L, "Jane Smith", "jane@example.com", 25));
        users.put(3L, new User(3L, "Bob Johnson", "bob@example.com", 35));
    }
    
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }
    
    public User findById(Long id) {
        return users.get(id);
    }
}

// Domain models
class Task {
    private Long id;
    
    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;
    
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
    
    @NotNull(message = "Priority is required")
    private TaskPriority priority;
    
    private TaskStatus status = TaskStatus.TODO;
    
    @NotNull(message = "Assignee is required")
    private Long assigneeId;
    
    private LocalDateTime dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }
    
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    
    public boolean isOverdue() {
        return dueDate != null && dueDate.isBefore(LocalDateTime.now()) && status != TaskStatus.COMPLETED;
    }
}

enum TaskStatus {
    TODO("To Do"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");
    
    private final String displayName;
    
    TaskStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

enum TaskPriority {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    URGENT("Urgent");
    
    private final String displayName;
    
    TaskPriority(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getBadgeClass() {
        return switch (this) {
            case LOW -> "bg-secondary";
            case MEDIUM -> "bg-primary";
            case HIGH -> "bg-warning";
            case URGENT -> "bg-danger";
        };
    }
}
```

---

## 🎯 Day 19 Summary

**Concepts Mastered:**
- ✅ **Spring MVC Architecture**: DispatcherServlet, controllers, views, and request flow
- ✅ **Web Controllers**: Request mapping, path variables, and method parameters
- ✅ **Thymeleaf Integration**: Template engine, fragments, and view rendering
- ✅ **Form Handling**: Data binding, validation, and form processing
- ✅ **Session Management**: HTTP sessions and state management
- ✅ **Complete Web Application**: Full-stack CRUD operations with MVC pattern

**Key Takeaways:**
1. **MVC Pattern** separates concerns between model, view, and controller
2. **Spring MVC** provides powerful request mapping and data binding capabilities
3. **Thymeleaf** enables server-side rendering with natural templates
4. **Form Validation** ensures data integrity with Bean Validation annotations
5. **Session Management** maintains state across HTTP requests
6. **Full-Stack Development** combines all components for complete web applications

**Enterprise Benefits:**
- **Separation of Concerns**: Clean architecture with distinct layers
- **Template Engine**: Server-side rendering with SEO benefits
- **Form Processing**: Robust data binding and validation
- **Session Management**: Stateful web application support
- **Responsive Design**: Bootstrap integration for modern UI

**Next Steps:**
- Day 20: Spring Data JPA for database integration
- Advanced web features: Security, internationalization, and caching
- RESTful web services with JSON/XML support
- Testing strategies for web applications

---

**🎉 Congratulations! You've mastered Spring MVC and built a complete web application with forms, validation, and CRUD operations!**