# Day 50: Creational Patterns - Singleton & Factory

## Learning Goals

By the end of this session, you will:
- Understand design patterns fundamentals and their importance in software development
- Master the Singleton pattern with thread-safe implementations
- Implement Factory Method and Abstract Factory patterns
- Build practical applications using creational patterns
- Create reusable and maintainable object creation solutions
- Understand when and why to use each creational pattern

## Table of Contents
1. [Design Patterns Introduction](#design-patterns-introduction)
2. [Singleton Pattern](#singleton-pattern)
3. [Factory Method Pattern](#factory-method-pattern)
4. [Abstract Factory Pattern](#abstract-factory-pattern)
5. [Hands-On Practice](#hands-on-practice)
6. [Challenges](#challenges)
7. [Summary](#summary)

---

## Design Patterns Introduction

### What are Design Patterns?

Design patterns are **proven solutions to recurring design problems** in software development. They represent best practices evolved over time by experienced developers.

**Benefits of Design Patterns:**
- **Reusability**: Solutions can be applied to similar problems
- **Communication**: Common vocabulary for developers
- **Best Practices**: Time-tested approaches to common problems
- **Maintainability**: Well-structured, organized code

### Gang of Four (GoF) Classification

Design patterns are classified into three categories:

1. **Creational Patterns**: Object creation mechanisms
   - Singleton, Factory Method, Abstract Factory, Builder, Prototype

2. **Structural Patterns**: Object composition and relationships
   - Adapter, Decorator, Facade, Composite, Proxy

3. **Behavioral Patterns**: Communication between objects
   - Observer, Strategy, Command, State, Template Method

---

## Singleton Pattern

### Problem and Solution

**Problem**: Ensure a class has only one instance and provide global access to it.

**Solution**: Control instantiation by making constructor private and providing a static method for access.

### Basic Singleton Implementation

```java
// BasicSingleton.java - Simple but not thread-safe
package com.javajourney.patterns.singleton;

/**
 * Basic Singleton implementation - NOT thread-safe
 * This is a demonstration of the simplest form
 */
public class BasicSingleton {
    
    // Private static instance
    private static BasicSingleton instance;
    
    // Private constructor prevents external instantiation
    private BasicSingleton() {
        System.out.println("BasicSingleton instance created");
    }
    
    // Public static method to get instance
    public static BasicSingleton getInstance() {
        if (instance == null) {
            instance = new BasicSingleton();
        }
        return instance;
    }
    
    public void doSomething() {
        System.out.println("BasicSingleton doing something...");
    }
}
```

### Thread-Safe Singleton Implementations

```java
// ThreadSafeSingleton.java - Various thread-safe implementations
package com.javajourney.patterns.singleton;

/**
 * Thread-Safe Singleton Implementations
 * Demonstrates different approaches to handle concurrency
 */

// 1. Synchronized Method Approach
class SynchronizedSingleton {
    private static SynchronizedSingleton instance;
    
    private SynchronizedSingleton() {}
    
    // Synchronized method - simple but performance overhead
    public static synchronized SynchronizedSingleton getInstance() {
        if (instance == null) {
            instance = new SynchronizedSingleton();
        }
        return instance;
    }
}

// 2. Double-Checked Locking Approach
class DoubleCheckedLockingSingleton {
    // volatile ensures instance is fully constructed before assignment
    private static volatile DoubleCheckedLockingSingleton instance;
    
    private DoubleCheckedLockingSingleton() {}
    
    public static DoubleCheckedLockingSingleton getInstance() {
        if (instance == null) {
            synchronized (DoubleCheckedLockingSingleton.class) {
                if (instance == null) {
                    instance = new DoubleCheckedLockingSingleton();
                }
            }
        }
        return instance;
    }
}

// 3. Bill Pugh Singleton (Initialization-on-demand holder)
class BillPughSingleton {
    private BillPughSingleton() {}
    
    // Inner class loaded only when getInstance() is called
    private static class SingletonHelper {
        private static final BillPughSingleton INSTANCE = new BillPughSingleton();
    }
    
    public static BillPughSingleton getInstance() {
        return SingletonHelper.INSTANCE;
    }
}

// 4. Enum Singleton (Best approach)
enum EnumSingleton {
    INSTANCE;
    
    public void doSomething() {
        System.out.println("EnumSingleton doing something...");
    }
}
```

### Practical Singleton: Configuration Manager

```java
// ConfigurationManager.java - Real-world singleton example
package com.javajourney.patterns.singleton;

import java.io.*;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Configuration Manager Singleton
 * Manages application configuration with thread-safety and lazy loading
 */
public class ConfigurationManager {
    
    private static volatile ConfigurationManager instance;
    private final Map<String, String> configCache;
    private final Properties properties;
    private final String configFilePath;
    
    private ConfigurationManager() {
        this.configCache = new ConcurrentHashMap<>();
        this.properties = new Properties();
        this.configFilePath = "application.properties";
        loadConfiguration();
    }
    
    public static ConfigurationManager getInstance() {
        if (instance == null) {
            synchronized (ConfigurationManager.class) {
                if (instance == null) {
                    instance = new ConfigurationManager();
                }
            }
        }
        return instance;
    }
    
    private void loadConfiguration() {
        try (InputStream input = ConfigurationManager.class
                .getClassLoader().getResourceAsStream(configFilePath)) {
            
            if (input != null) {
                properties.load(input);
                System.out.println("Configuration loaded successfully");
            } else {
                System.out.println("Configuration file not found, using defaults");
                setDefaultConfiguration();
            }
            
        } catch (IOException e) {
            System.err.println("Error loading configuration: " + e.getMessage());
            setDefaultConfiguration();
        }
    }
    
    private void setDefaultConfiguration() {
        properties.setProperty("app.name", "Java Learning Journey");
        properties.setProperty("app.version", "1.0.0");
        properties.setProperty("database.url", "jdbc:h2:mem:testdb");
        properties.setProperty("database.driver", "org.h2.Driver");
        properties.setProperty("logging.level", "INFO");
    }
    
    public String getProperty(String key) {
        return configCache.computeIfAbsent(key, k -> properties.getProperty(k));
    }
    
    public String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value != null ? value : defaultValue;
    }
    
    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
        configCache.put(key, value);
    }
    
    public boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = getProperty(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }
    
    public int getIntProperty(String key, int defaultValue) {
        String value = getProperty(key);
        try {
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    public void saveConfiguration() {
        try (OutputStream output = new FileOutputStream(configFilePath)) {
            properties.store(output, "Application Configuration");
            System.out.println("Configuration saved successfully");
        } catch (IOException e) {
            System.err.println("Error saving configuration: " + e.getMessage());
        }
    }
    
    public void printAllProperties() {
        System.out.println("=== Configuration Properties ===");
        properties.forEach((key, value) -> 
            System.out.println(key + " = " + value));
    }
}
```

---

## Factory Method Pattern

### Problem and Solution

**Problem**: Create objects without specifying their exact classes, allowing subclasses to decide which class to instantiate.

**Solution**: Define an interface for creating objects, but let subclasses decide which class to instantiate.

### Basic Factory Method Implementation

```java
// VehicleFactory.java - Factory Method Pattern implementation
package com.javajourney.patterns.factory;

/**
 * Vehicle Factory Method Pattern Implementation
 * Demonstrates object creation without specifying exact classes
 */

// Product interface
interface Vehicle {
    void start();
    void stop();
    String getType();
    double getMaxSpeed();
}

// Concrete Products
class Car implements Vehicle {
    private String model;
    private double maxSpeed;
    
    public Car(String model) {
        this.model = model;
        this.maxSpeed = 200.0;
    }
    
    @Override
    public void start() {
        System.out.println("Car " + model + " engine started");
    }
    
    @Override
    public void stop() {
        System.out.println("Car " + model + " engine stopped");
    }
    
    @Override
    public String getType() {
        return "Car - " + model;
    }
    
    @Override
    public double getMaxSpeed() {
        return maxSpeed;
    }
}

class Motorcycle implements Vehicle {
    private String brand;
    private double maxSpeed;
    
    public Motorcycle(String brand) {
        this.brand = brand;
        this.maxSpeed = 180.0;
    }
    
    @Override
    public void start() {
        System.out.println("Motorcycle " + brand + " engine started");
    }
    
    @Override
    public void stop() {
        System.out.println("Motorcycle " + brand + " engine stopped");
    }
    
    @Override
    public String getType() {
        return "Motorcycle - " + brand;
    }
    
    @Override
    public double getMaxSpeed() {
        return maxSpeed;
    }
}

class Truck implements Vehicle {
    private String type;
    private double maxSpeed;
    
    public Truck(String type) {
        this.type = type;
        this.maxSpeed = 120.0;
    }
    
    @Override
    public void start() {
        System.out.println("Truck " + type + " engine started");
    }
    
    @Override
    public void stop() {
        System.out.println("Truck " + type + " engine stopped");
    }
    
    @Override
    public String getType() {
        return "Truck - " + type;
    }
    
    @Override
    public double getMaxSpeed() {
        return maxSpeed;
    }
}

// Creator (Factory) class
abstract class VehicleFactory {
    
    // Factory method - subclasses will implement this
    public abstract Vehicle createVehicle(String model);
    
    // Template method using factory method
    public Vehicle orderVehicle(String model) {
        Vehicle vehicle = createVehicle(model);
        
        // Common operations for all vehicles
        System.out.println("Manufacturing " + vehicle.getType());
        System.out.println("Quality check passed");
        System.out.println("Vehicle ready for delivery");
        
        return vehicle;
    }
}

// Concrete Factories
class CarFactory extends VehicleFactory {
    @Override
    public Vehicle createVehicle(String model) {
        return new Car(model);
    }
}

class MotorcycleFactory extends VehicleFactory {
    @Override
    public Vehicle createVehicle(String brand) {
        return new Motorcycle(brand);
    }
}

class TruckFactory extends VehicleFactory {
    @Override
    public Vehicle createVehicle(String type) {
        return new Truck(type);
    }
}
```

### Advanced Factory: Database Connection Factory

```java
// DatabaseConnectionFactory.java - Advanced factory implementation
package com.javajourney.patterns.factory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Database Connection Factory
 * Demonstrates factory pattern for database connections
 */

// Database connection interface
interface DatabaseConnection {
    Connection getConnection() throws SQLException;
    String getConnectionInfo();
    void closeConnection();
    boolean testConnection();
}

// MySQL connection implementation
class MySQLConnection implements DatabaseConnection {
    private Connection connection;
    private final String url;
    private final String username;
    private final String password;
    
    public MySQLConnection(String host, int port, String database, 
                          String username, String password) {
        this.url = String.format("jdbc:mysql://%s:%d/%s", host, port, database);
        this.username = username;
        this.password = password;
    }
    
    @Override
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(url, username, password);
            System.out.println("MySQL connection established");
        }
        return connection;
    }
    
    @Override
    public String getConnectionInfo() {
        return "MySQL Database: " + url;
    }
    
    @Override
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("MySQL connection closed");
            }
        } catch (SQLException e) {
            System.err.println("Error closing MySQL connection: " + e.getMessage());
        }
    }
    
    @Override
    public boolean testConnection() {
        try {
            getConnection();
            return !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}

// PostgreSQL connection implementation
class PostgreSQLConnection implements DatabaseConnection {
    private Connection connection;
    private final String url;
    private final Properties properties;
    
    public PostgreSQLConnection(String host, int port, String database, 
                               String username, String password) {
        this.url = String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
        this.properties = new Properties();
        properties.setProperty("user", username);
        properties.setProperty("password", password);
    }
    
    @Override
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(url, properties);
            System.out.println("PostgreSQL connection established");
        }
        return connection;
    }
    
    @Override
    public String getConnectionInfo() {
        return "PostgreSQL Database: " + url;
    }
    
    @Override
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("PostgreSQL connection closed");
            }
        } catch (SQLException e) {
            System.err.println("Error closing PostgreSQL connection: " + e.getMessage());
        }
    }
    
    @Override
    public boolean testConnection() {
        try {
            getConnection();
            return !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}

// H2 in-memory database connection
class H2Connection implements DatabaseConnection {
    private Connection connection;
    private final String url;
    
    public H2Connection(String databaseName) {
        this.url = "jdbc:h2:mem:" + databaseName + ";DB_CLOSE_DELAY=-1";
    }
    
    @Override
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(url, "sa", "");
            System.out.println("H2 in-memory connection established");
        }
        return connection;
    }
    
    @Override
    public String getConnectionInfo() {
        return "H2 In-Memory Database: " + url;
    }
    
    @Override
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("H2 connection closed");
            }
        } catch (SQLException e) {
            System.err.println("Error closing H2 connection: " + e.getMessage());
        }
    }
    
    @Override
    public boolean testConnection() {
        try {
            getConnection();
            return !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}

// Database connection factory
public class DatabaseConnectionFactory {
    
    public enum DatabaseType {
        MYSQL, POSTGRESQL, H2
    }
    
    public static DatabaseConnection createConnection(DatabaseType type, 
                                                    String host, int port, 
                                                    String database, 
                                                    String username, 
                                                    String password) {
        
        return switch (type) {
            case MYSQL -> new MySQLConnection(host, port, database, username, password);
            case POSTGRESQL -> new PostgreSQLConnection(host, port, database, username, password);
            case H2 -> new H2Connection(database);
        };
    }
    
    // Convenience method for H2 in-memory database
    public static DatabaseConnection createH2Connection(String databaseName) {
        return new H2Connection(databaseName);
    }
    
    // Create connection from configuration
    public static DatabaseConnection createFromConfig(DatabaseType type) {
        ConfigurationManager config = ConfigurationManager.getInstance();
        
        String host = config.getProperty("database.host", "localhost");
        int port = config.getIntProperty("database.port", getDefaultPort(type));
        String database = config.getProperty("database.name", "testdb");
        String username = config.getProperty("database.username", "user");
        String password = config.getProperty("database.password", "password");
        
        return createConnection(type, host, port, database, username, password);
    }
    
    private static int getDefaultPort(DatabaseType type) {
        return switch (type) {
            case MYSQL -> 3306;
            case POSTGRESQL -> 5432;
            case H2 -> 0; // Not applicable for in-memory
        };
    }
}
```

---

## Abstract Factory Pattern

### Problem and Solution

**Problem**: Provide an interface for creating families of related objects without specifying their concrete classes.

**Solution**: Create an abstract factory interface that declares creation methods for each type of product in the family.

### Cross-Platform UI Abstract Factory

```java
// CrossPlatformUIFactory.java - Abstract Factory Pattern implementation
package com.javajourney.patterns.abstractfactory;

/**
 * Cross-Platform UI Abstract Factory Implementation
 * Demonstrates creation of families of related UI components
 */

// Abstract Products
interface Button {
    void render();
    void onClick();
    String getStyle();
}

interface TextField {
    void render();
    void setText(String text);
    String getText();
    String getStyle();
}

interface CheckBox {
    void render();
    void setChecked(boolean checked);
    boolean isChecked();
    String getStyle();
}

// Windows UI Components
class WindowsButton implements Button {
    private String text;
    
    public WindowsButton(String text) {
        this.text = text;
    }
    
    @Override
    public void render() {
        System.out.println("Rendering Windows-style button: " + text);
    }
    
    @Override
    public void onClick() {
        System.out.println("Windows button clicked: " + text);
    }
    
    @Override
    public String getStyle() {
        return "Windows Button Style";
    }
}

class WindowsTextField implements TextField {
    private String text = "";
    
    @Override
    public void render() {
        System.out.println("Rendering Windows-style text field");
    }
    
    @Override
    public void setText(String text) {
        this.text = text;
        System.out.println("Windows text field set to: " + text);
    }
    
    @Override
    public String getText() {
        return text;
    }
    
    @Override
    public String getStyle() {
        return "Windows TextField Style";
    }
}

class WindowsCheckBox implements CheckBox {
    private boolean checked = false;
    private String label;
    
    public WindowsCheckBox(String label) {
        this.label = label;
    }
    
    @Override
    public void render() {
        System.out.println("Rendering Windows-style checkbox: " + label);
    }
    
    @Override
    public void setChecked(boolean checked) {
        this.checked = checked;
        System.out.println("Windows checkbox " + label + " set to: " + checked);
    }
    
    @Override
    public boolean isChecked() {
        return checked;
    }
    
    @Override
    public String getStyle() {
        return "Windows CheckBox Style";
    }
}

// macOS UI Components
class MacOSButton implements Button {
    private String text;
    
    public MacOSButton(String text) {
        this.text = text;
    }
    
    @Override
    public void render() {
        System.out.println("Rendering macOS-style button: " + text);
    }
    
    @Override
    public void onClick() {
        System.out.println("macOS button clicked: " + text);
    }
    
    @Override
    public String getStyle() {
        return "macOS Button Style";
    }
}

class MacOSTextField implements TextField {
    private String text = "";
    
    @Override
    public void render() {
        System.out.println("Rendering macOS-style text field");
    }
    
    @Override
    public void setText(String text) {
        this.text = text;
        System.out.println("macOS text field set to: " + text);
    }
    
    @Override
    public String getText() {
        return text;
    }
    
    @Override
    public String getStyle() {
        return "macOS TextField Style";
    }
}

class MacOSCheckBox implements CheckBox {
    private boolean checked = false;
    private String label;
    
    public MacOSCheckBox(String label) {
        this.label = label;
    }
    
    @Override
    public void render() {
        System.out.println("Rendering macOS-style checkbox: " + label);
    }
    
    @Override
    public void setChecked(boolean checked) {
        this.checked = checked;
        System.out.println("macOS checkbox " + label + " set to: " + checked);
    }
    
    @Override
    public boolean isChecked() {
        return checked;
    }
    
    @Override
    public String getStyle() {
        return "macOS CheckBox Style";
    }
}

// Linux UI Components
class LinuxButton implements Button {
    private String text;
    
    public LinuxButton(String text) {
        this.text = text;
    }
    
    @Override
    public void render() {
        System.out.println("Rendering Linux-style button: " + text);
    }
    
    @Override
    public void onClick() {
        System.out.println("Linux button clicked: " + text);
    }
    
    @Override
    public String getStyle() {
        return "Linux Button Style";
    }
}

class LinuxTextField implements TextField {
    private String text = "";
    
    @Override
    public void render() {
        System.out.println("Rendering Linux-style text field");
    }
    
    @Override
    public void setText(String text) {
        this.text = text;
        System.out.println("Linux text field set to: " + text);
    }
    
    @Override
    public String getText() {
        return text;
    }
    
    @Override
    public String getStyle() {
        return "Linux TextField Style";
    }
}

class LinuxCheckBox implements CheckBox {
    private boolean checked = false;
    private String label;
    
    public LinuxCheckBox(String label) {
        this.label = label;
    }
    
    @Override
    public void render() {
        System.out.println("Rendering Linux-style checkbox: " + label);
    }
    
    @Override
    public void setChecked(boolean checked) {
        this.checked = checked;
        System.out.println("Linux checkbox " + label + " set to: " + checked);
    }
    
    @Override
    public boolean isChecked() {
        return checked;
    }
    
    @Override
    public String getStyle() {
        return "Linux CheckBox Style";
    }
}

// Abstract Factory
interface UIFactory {
    Button createButton(String text);
    TextField createTextField();
    CheckBox createCheckBox(String label);
    String getThemeName();
}

// Concrete Factories
class WindowsUIFactory implements UIFactory {
    @Override
    public Button createButton(String text) {
        return new WindowsButton(text);
    }
    
    @Override
    public TextField createTextField() {
        return new WindowsTextField();
    }
    
    @Override
    public CheckBox createCheckBox(String label) {
        return new WindowsCheckBox(label);
    }
    
    @Override
    public String getThemeName() {
        return "Windows Theme";
    }
}

class MacOSUIFactory implements UIFactory {
    @Override
    public Button createButton(String text) {
        return new MacOSButton(text);
    }
    
    @Override
    public TextField createTextField() {
        return new MacOSTextField();
    }
    
    @Override
    public CheckBox createCheckBox(String label) {
        return new MacOSCheckBox(label);
    }
    
    @Override
    public String getThemeName() {
        return "macOS Theme";
    }
}

class LinuxUIFactory implements UIFactory {
    @Override
    public Button createButton(String text) {
        return new LinuxButton(text);
    }
    
    @Override
    public TextField createTextField() {
        return new LinuxTextField();
    }
    
    @Override
    public CheckBox createCheckBox(String label) {
        return new LinuxCheckBox(label);
    }
    
    @Override
    public String getThemeName() {
        return "Linux Theme";
    }
}

// Factory Provider
public class UIFactoryProvider {
    
    public enum Platform {
        WINDOWS, MACOS, LINUX
    }
    
    public static UIFactory getFactory(Platform platform) {
        return switch (platform) {
            case WINDOWS -> new WindowsUIFactory();
            case MACOS -> new MacOSUIFactory();
            case LINUX -> new LinuxUIFactory();
        };
    }
    
    // Auto-detect platform and return appropriate factory
    public static UIFactory getFactory() {
        String osName = System.getProperty("os.name").toLowerCase();
        
        if (osName.contains("windows")) {
            return new WindowsUIFactory();
        } else if (osName.contains("mac")) {
            return new MacOSUIFactory();
        } else {
            return new LinuxUIFactory();
        }
    }
}

// Application using the Abstract Factory
class Application {
    private Button button;
    private TextField textField;
    private CheckBox checkBox;
    private UIFactory factory;
    
    public Application(UIFactory factory) {
        this.factory = factory;
        createUI();
    }
    
    private void createUI() {
        button = factory.createButton("Submit");
        textField = factory.createTextField();
        checkBox = factory.createCheckBox("Remember me");
        
        System.out.println("UI created using: " + factory.getThemeName());
    }
    
    public void renderUI() {
        System.out.println("=== Rendering Application UI ===");
        button.render();
        textField.render();
        checkBox.render();
    }
    
    public void simulateUserInteraction() {
        System.out.println("=== Simulating User Interaction ===");
        textField.setText("user@example.com");
        checkBox.setChecked(true);
        button.onClick();
        
        System.out.println("Text field value: " + textField.getText());
        System.out.println("Checkbox checked: " + checkBox.isChecked());
    }
}
```

---

## Hands-On Practice

### Practice 1: Logger Factory

```java
// LoggerFactory.java - Create a flexible logging system
package com.javajourney.patterns.practice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.*;

/**
 * Logger Factory Practice
 * Implement different types of loggers using Factory pattern
 */

// Logger interface
interface Logger {
    void log(LogLevel level, String message);
    void info(String message);
    void warn(String message);
    void error(String message);
    void debug(String message);
}

enum LogLevel {
    DEBUG, INFO, WARN, ERROR
}

// Console Logger implementation
class ConsoleLogger implements Logger {
    private final String name;
    private final DateTimeFormatter formatter;
    
    public ConsoleLogger(String name) {
        this.name = name;
        this.formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    }
    
    @Override
    public void log(LogLevel level, String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        System.out.printf("[%s] %s [%s] %s%n", timestamp, level, name, message);
    }
    
    @Override
    public void info(String message) { log(LogLevel.INFO, message); }
    
    @Override
    public void warn(String message) { log(LogLevel.WARN, message); }
    
    @Override
    public void error(String message) { log(LogLevel.ERROR, message); }
    
    @Override
    public void debug(String message) { log(LogLevel.DEBUG, message); }
}

// File Logger implementation
class FileLogger implements Logger {
    private final String name;
    private final String fileName;
    private final DateTimeFormatter formatter;
    
    public FileLogger(String name, String fileName) {
        this.name = name;
        this.fileName = fileName;
        this.formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    }
    
    @Override
    public void log(LogLevel level, String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        String logEntry = String.format("[%s] %s [%s] %s%n", timestamp, level, name, message);
        
        try (FileWriter writer = new FileWriter(fileName, true)) {
            writer.write(logEntry);
        } catch (IOException e) {
            System.err.println("Error writing to log file: " + e.getMessage());
        }
    }
    
    @Override
    public void info(String message) { log(LogLevel.INFO, message); }
    
    @Override
    public void warn(String message) { log(LogLevel.WARN, message); }
    
    @Override
    public void error(String message) { log(LogLevel.ERROR, message); }
    
    @Override
    public void debug(String message) { log(LogLevel.DEBUG, message); }
}

// Composite Logger (logs to multiple destinations)
class CompositeLogger implements Logger {
    private final String name;
    private final Logger[] loggers;
    
    public CompositeLogger(String name, Logger... loggers) {
        this.name = name;
        this.loggers = loggers;
    }
    
    @Override
    public void log(LogLevel level, String message) {
        for (Logger logger : loggers) {
            logger.log(level, message);
        }
    }
    
    @Override
    public void info(String message) { log(LogLevel.INFO, message); }
    
    @Override
    public void warn(String message) { log(LogLevel.WARN, message); }
    
    @Override
    public void error(String message) { log(LogLevel.ERROR, message); }
    
    @Override
    public void debug(String message) { log(LogLevel.DEBUG, message); }
}

// Logger Factory
public class LoggerFactory {
    
    public enum LoggerType {
        CONSOLE, FILE, COMPOSITE
    }
    
    public static Logger createLogger(LoggerType type, String name, String... args) {
        return switch (type) {
            case CONSOLE -> new ConsoleLogger(name);
            case FILE -> {
                String fileName = args.length > 0 ? args[0] : name + ".log";
                yield new FileLogger(name, fileName);
            }
            case COMPOSITE -> {
                Logger consoleLogger = new ConsoleLogger(name);
                String fileName = args.length > 0 ? args[0] : name + ".log";
                Logger fileLogger = new FileLogger(name, fileName);
                yield new CompositeLogger(name, consoleLogger, fileLogger);
            }
        };
    }
    
    // Convenience methods
    public static Logger createConsoleLogger(String name) {
        return createLogger(LoggerType.CONSOLE, name);
    }
    
    public static Logger createFileLogger(String name, String fileName) {
        return createLogger(LoggerType.FILE, name, fileName);
    }
    
    public static Logger createCompositeLogger(String name, String fileName) {
        return createLogger(LoggerType.COMPOSITE, name, fileName);
    }
}
```

### Practice 2: Comprehensive Example Application

```java
// PatternsDemo.java - Comprehensive demonstration of creational patterns
package com.javajourney.patterns;

/**
 * Comprehensive Demonstration of Creational Patterns
 * Shows practical usage of Singleton, Factory Method, and Abstract Factory patterns
 */
public class PatternsDemo {
    
    public static void main(String[] args) {
        demonstrateSingleton();
        System.out.println();
        
        demonstrateFactoryMethod();
        System.out.println();
        
        demonstrateAbstractFactory();
        System.out.println();
        
        demonstrateLoggerFactory();
    }
    
    private static void demonstrateSingleton() {
        System.out.println("=== Singleton Pattern Demo ===");
        
        // Get configuration instance
        ConfigurationManager config = ConfigurationManager.getInstance();
        config.printAllProperties();
        
        // Verify singleton behavior
        ConfigurationManager config2 = ConfigurationManager.getInstance();
        System.out.println("Same instance? " + (config == config2));
        
        // Use configuration
        String appName = config.getProperty("app.name");
        System.out.println("Application: " + appName);
        
        // Enum singleton demo
        EnumSingleton.INSTANCE.doSomething();
    }
    
    private static void demonstrateFactoryMethod() {
        System.out.println("=== Factory Method Pattern Demo ===");
        
        // Create different vehicle factories
        VehicleFactory carFactory = new CarFactory();
        VehicleFactory motorcycleFactory = new MotorcycleFactory();
        VehicleFactory truckFactory = new TruckFactory();
        
        // Order different vehicles
        Vehicle car = carFactory.orderVehicle("Tesla Model 3");
        Vehicle motorcycle = motorcycleFactory.orderVehicle("Harley Davidson");
        Vehicle truck = truckFactory.orderVehicle("Delivery Truck");
        
        // Test vehicles
        System.out.println("\nTesting vehicles:");
        testVehicle(car);
        testVehicle(motorcycle);
        testVehicle(truck);
        
        // Database connection factory demo
        DatabaseConnection h2Connection = DatabaseConnectionFactory.createH2Connection("testdb");
        System.out.println("Created: " + h2Connection.getConnectionInfo());
        System.out.println("Connection test: " + h2Connection.testConnection());
    }
    
    private static void demonstrateAbstractFactory() {
        System.out.println("=== Abstract Factory Pattern Demo ===");
        
        // Auto-detect platform
        UIFactory autoFactory = UIFactoryProvider.getFactory();
        System.out.println("Auto-detected: " + autoFactory.getThemeName());
        
        // Create application with auto-detected UI
        Application autoApp = new Application(autoFactory);
        autoApp.renderUI();
        autoApp.simulateUserInteraction();
        
        System.out.println();
        
        // Test different platforms
        for (UIFactoryProvider.Platform platform : UIFactoryProvider.Platform.values()) {
            System.out.println("--- Testing " + platform + " ---");
            UIFactory factory = UIFactoryProvider.getFactory(platform);
            Application app = new Application(factory);
            app.renderUI();
        }
    }
    
    private static void demonstrateLoggerFactory() {
        System.out.println("=== Logger Factory Demo ===");
        
        // Create different types of loggers
        Logger consoleLogger = LoggerFactory.createConsoleLogger("ConsoleApp");
        Logger fileLogger = LoggerFactory.createFileLogger("FileApp", "application.log");
        Logger compositeLogger = LoggerFactory.createCompositeLogger("CompositeApp", "composite.log");
        
        // Test loggers
        testLogger("Console Logger", consoleLogger);
        testLogger("File Logger", fileLogger);
        testLogger("Composite Logger", compositeLogger);
    }
    
    private static void testVehicle(Vehicle vehicle) {
        System.out.println("Testing: " + vehicle.getType());
        vehicle.start();
        System.out.println("Max speed: " + vehicle.getMaxSpeed() + " km/h");
        vehicle.stop();
        System.out.println();
    }
    
    private static void testLogger(String loggerName, Logger logger) {
        System.out.println("Testing: " + loggerName);
        logger.info("Application started");
        logger.warn("This is a warning message");
        logger.error("This is an error message");
        logger.debug("Debug information");
        System.out.println();
    }
}
```

---

## Challenges

### Challenge 1: Theme Factory
Create an abstract factory for different application themes (Dark, Light, High Contrast) that produces Color, Font, and Icon objects.

### Challenge 2: Connection Pool Singleton
Implement a thread-safe connection pool singleton that manages database connections with proper resource cleanup.

### Challenge 3: Plugin Factory
Design a factory system that can dynamically load and create plugin instances based on configuration files.

---

## Summary

### Key Takeaways

1. **Design Patterns Benefits**:
   - Proven solutions to common problems
   - Improved code organization and maintainability
   - Better communication among developers
   - Reusable design solutions

2. **Singleton Pattern**:
   - Ensures single instance with global access
   - Thread-safety considerations are crucial
   - Enum implementation is often the best choice
   - Use for configuration, logging, caching

3. **Factory Method Pattern**:
   - Creates objects without specifying exact classes
   - Promotes loose coupling between creator and products
   - Enables easy extension with new product types
   - Template method pattern integration

4. **Abstract Factory Pattern**:
   - Creates families of related objects
   - Ensures consistency among created objects
   - Easy to switch between different families
   - Platform-specific implementations

### Performance Checklist
- [ ] Singleton instances are thread-safe
- [ ] Factory methods handle invalid inputs gracefully
- [ ] Resource cleanup is properly implemented
- [ ] Error handling covers all edge cases
- [ ] Memory leaks are prevented

### Quality Gates Checklist
- [ ] All patterns follow SOLID principles
- [ ] Code is well-documented with clear examples
- [ ] Unit tests cover all pattern implementations
- [ ] Exception handling is comprehensive
- [ ] Resource management is proper

### Tomorrow's Preview
Tomorrow we'll explore **Creational Patterns - Builder & Prototype** where we'll learn to construct complex objects step-by-step and create new objects by cloning existing instances.

---

*Day 50 Complete! You've mastered the fundamental creational patterns. You can now create flexible object creation systems using Singleton, Factory Method, and Abstract Factory patterns. Tomorrow we'll dive deeper into more sophisticated creational patterns.*