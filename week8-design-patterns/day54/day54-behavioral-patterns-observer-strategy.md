# Day 54: Behavioral Patterns - Observer & Strategy

## Learning Objectives
- Master the Observer pattern for event-driven programming
- Understand the Strategy pattern for algorithm encapsulation
- Learn when and how to apply these patterns
- Build practical examples with real-world scenarios

---

## 1. Observer Pattern

The Observer pattern defines a one-to-many dependency between objects so that when one object changes state, all its dependents are notified and updated automatically.

### Key Concepts:
- **Subject**: The object being observed
- **Observer**: Objects that want to be notified of changes
- **Decoupling**: Subject doesn't need to know concrete observer classes
- **Dynamic Relationships**: Observers can be added/removed at runtime

### Example 1: Stock Price Monitoring System

```java
import java.util.*;

// Observer interface
interface StockObserver {
    void update(String stockSymbol, double price);
}

// Subject interface
interface StockSubject {
    void addObserver(StockObserver observer);
    void removeObserver(StockObserver observer);
    void notifyObservers();
}

// Concrete Subject
class StockPrice implements StockSubject {
    private List<StockObserver> observers = new ArrayList<>();
    private String stockSymbol;
    private double price;
    
    public StockPrice(String stockSymbol) {
        this.stockSymbol = stockSymbol;
    }
    
    @Override
    public void addObserver(StockObserver observer) {
        observers.add(observer);
        System.out.println("Observer added for " + stockSymbol);
    }
    
    @Override
    public void removeObserver(StockObserver observer) {
        observers.remove(observer);
        System.out.println("Observer removed for " + stockSymbol);
    }
    
    @Override
    public void notifyObservers() {
        for (StockObserver observer : observers) {
            observer.update(stockSymbol, price);
        }
    }
    
    public void setPrice(double price) {
        this.price = price;
        System.out.println(stockSymbol + " price changed to $" + price);
        notifyObservers();
    }
    
    public double getPrice() {
        return price;
    }
    
    public String getStockSymbol() {
        return stockSymbol;
    }
}

// Concrete Observer - Mobile App
class MobileApp implements StockObserver {
    private String appName;
    
    public MobileApp(String appName) {
        this.appName = appName;
    }
    
    @Override
    public void update(String stockSymbol, double price) {
        System.out.println("📱 " + appName + " - Push notification: " + stockSymbol + " is now $" + price);
    }
}

// Concrete Observer - Email Service
class EmailService implements StockObserver {
    private String emailAddress;
    
    public EmailService(String emailAddress) {
        this.emailAddress = emailAddress;
    }
    
    @Override
    public void update(String stockSymbol, double price) {
        System.out.println("📧 Email sent to " + emailAddress + ": " + stockSymbol + " price alert: $" + price);
    }
}

// Concrete Observer - Trading Bot
class TradingBot implements StockObserver {
    private String botName;
    private double buyThreshold;
    private double sellThreshold;
    
    public TradingBot(String botName, double buyThreshold, double sellThreshold) {
        this.botName = botName;
        this.buyThreshold = buyThreshold;
        this.sellThreshold = sellThreshold;
    }
    
    @Override
    public void update(String stockSymbol, double price) {
        if (price <= buyThreshold) {
            System.out.println("🤖 " + botName + " - BUY signal for " + stockSymbol + " at $" + price);
        } else if (price >= sellThreshold) {
            System.out.println("🤖 " + botName + " - SELL signal for " + stockSymbol + " at $" + price);
        } else {
            System.out.println("🤖 " + botName + " - HOLD for " + stockSymbol + " at $" + price);
        }
    }
}

// Usage
public class ObserverPatternDemo {
    public static void main(String[] args) {
        // Create stock price subject
        StockPrice appleStock = new StockPrice("AAPL");
        
        // Create observers
        MobileApp tradingApp = new MobileApp("TradingApp");
        EmailService emailService = new EmailService("investor@example.com");
        TradingBot bot = new TradingBot("AlgoBot", 150.0, 200.0);
        
        // Add observers
        appleStock.addObserver(tradingApp);
        appleStock.addObserver(emailService);
        appleStock.addObserver(bot);
        
        System.out.println("\n--- Stock Price Updates ---");
        // Update stock prices
        appleStock.setPrice(145.0);
        System.out.println();
        
        appleStock.setPrice(155.0);
        System.out.println();
        
        appleStock.setPrice(205.0);
        System.out.println();
        
        // Remove an observer
        appleStock.removeObserver(emailService);
        appleStock.setPrice(175.0);
    }
}
```

### Example 2: News Agency System

```java
import java.util.*;

// Observer interface
interface NewsObserver {
    void update(String headline, String content);
}

// Subject class
class NewsAgency {
    private List<NewsObserver> observers = new ArrayList<>();
    private String headline;
    private String content;
    
    public void addObserver(NewsObserver observer) {
        observers.add(observer);
    }
    
    public void removeObserver(NewsObserver observer) {
        observers.remove(observer);
    }
    
    public void notifyObservers() {
        for (NewsObserver observer : observers) {
            observer.update(headline, content);
        }
    }
    
    public void publishNews(String headline, String content) {
        this.headline = headline;
        this.content = content;
        System.out.println("📰 NEWS AGENCY: Publishing - " + headline);
        notifyObservers();
    }
}

// Concrete Observer - News Website
class NewsWebsite implements NewsObserver {
    private String websiteName;
    
    public NewsWebsite(String websiteName) {
        this.websiteName = websiteName;
    }
    
    @Override
    public void update(String headline, String content) {
        System.out.println("🌐 " + websiteName + " - Publishing: " + headline);
        System.out.println("   Content: " + content.substring(0, Math.min(50, content.length())) + "...");
    }
}

// Concrete Observer - TV Channel
class TVChannel implements NewsObserver {
    private String channelName;
    
    public TVChannel(String channelName) {
        this.channelName = channelName;
    }
    
    @Override
    public void update(String headline, String content) {
        System.out.println("📺 " + channelName + " - Breaking News: " + headline);
        System.out.println("   Broadcasting now...");
    }
}

// Concrete Observer - Social Media Platform
class SocialMediaPlatform implements NewsObserver {
    private String platformName;
    
    public SocialMediaPlatform(String platformName) {
        this.platformName = platformName;
    }
    
    @Override
    public void update(String headline, String content) {
        System.out.println("📱 " + platformName + " - Trending: " + headline);
        System.out.println("   #BreakingNews #" + headline.replace(" ", ""));
    }
}

// Usage
public class NewsAgencyDemo {
    public static void main(String[] args) {
        NewsAgency agency = new NewsAgency();
        
        // Create observers
        NewsWebsite cnn = new NewsWebsite("CNN.com");
        TVChannel fox = new TVChannel("Fox News");
        SocialMediaPlatform twitter = new SocialMediaPlatform("Twitter");
        
        // Add observers
        agency.addObserver(cnn);
        agency.addObserver(fox);
        agency.addObserver(twitter);
        
        // Publish news
        agency.publishNews("Tech Stock Surge", "Major technology stocks are experiencing unprecedented growth today as investors show confidence in the sector's future prospects.");
        
        System.out.println("\n" + "=".repeat(50) + "\n");
        
        agency.publishNews("Weather Alert", "Severe weather conditions expected in the northeastern region with heavy rainfall and strong winds forecasted for the next 48 hours.");
    }
}
```

---

## 2. Strategy Pattern

The Strategy pattern defines a family of algorithms, encapsulates each one, and makes them interchangeable. Strategy lets the algorithm vary independently from clients that use it.

### Key Concepts:
- **Strategy**: Interface defining the algorithm
- **ConcreteStrategy**: Specific implementations of algorithms
- **Context**: Uses a strategy to execute an algorithm
- **Runtime Selection**: Algorithms can be selected at runtime

### Example 1: Payment Processing System

```java
// Strategy interface
interface PaymentStrategy {
    void pay(double amount);
    boolean validate();
}

// Concrete Strategy - Credit Card
class CreditCardPayment implements PaymentStrategy {
    private String cardNumber;
    private String cardHolder;
    private String expiryDate;
    private String cvv;
    
    public CreditCardPayment(String cardNumber, String cardHolder, String expiryDate, String cvv) {
        this.cardNumber = cardNumber;
        this.cardHolder = cardHolder;
        this.expiryDate = expiryDate;
        this.cvv = cvv;
    }
    
    @Override
    public boolean validate() {
        // Simulate validation
        if (cardNumber.length() != 16) {
            System.out.println("❌ Invalid card number");
            return false;
        }
        if (cvv.length() != 3) {
            System.out.println("❌ Invalid CVV");
            return false;
        }
        System.out.println("✅ Credit card validated");
        return true;
    }
    
    @Override
    public void pay(double amount) {
        if (validate()) {
            System.out.println("💳 Paid $" + amount + " using Credit Card ending in " + 
                             cardNumber.substring(cardNumber.length() - 4));
            System.out.println("   Card holder: " + cardHolder);
        }
    }
}

// Concrete Strategy - PayPal
class PayPalPayment implements PaymentStrategy {
    private String email;
    private String password;
    
    public PayPalPayment(String email, String password) {
        this.email = email;
        this.password = password;
    }
    
    @Override
    public boolean validate() {
        // Simulate validation
        if (!email.contains("@")) {
            System.out.println("❌ Invalid email format");
            return false;
        }
        if (password.length() < 6) {
            System.out.println("❌ Password too short");
            return false;
        }
        System.out.println("✅ PayPal account validated");
        return true;
    }
    
    @Override
    public void pay(double amount) {
        if (validate()) {
            System.out.println("💰 Paid $" + amount + " using PayPal");
            System.out.println("   Account: " + email);
        }
    }
}

// Concrete Strategy - Bank Transfer
class BankTransferPayment implements PaymentStrategy {
    private String accountNumber;
    private String routingNumber;
    private String bankName;
    
    public BankTransferPayment(String accountNumber, String routingNumber, String bankName) {
        this.accountNumber = accountNumber;
        this.routingNumber = routingNumber;
        this.bankName = bankName;
    }
    
    @Override
    public boolean validate() {
        // Simulate validation
        if (accountNumber.length() < 8) {
            System.out.println("❌ Invalid account number");
            return false;
        }
        if (routingNumber.length() != 9) {
            System.out.println("❌ Invalid routing number");
            return false;
        }
        System.out.println("✅ Bank account validated");
        return true;
    }
    
    @Override
    public void pay(double amount) {
        if (validate()) {
            System.out.println("🏦 Paid $" + amount + " using Bank Transfer");
            System.out.println("   Bank: " + bankName);
            System.out.println("   Account: ***" + accountNumber.substring(accountNumber.length() - 4));
        }
    }
}

// Context class
class PaymentContext {
    private PaymentStrategy paymentStrategy;
    
    public void setPaymentStrategy(PaymentStrategy paymentStrategy) {
        this.paymentStrategy = paymentStrategy;
    }
    
    public void processPayment(double amount) {
        if (paymentStrategy == null) {
            System.out.println("❌ No payment method selected");
            return;
        }
        
        System.out.println("💸 Processing payment of $" + amount);
        paymentStrategy.pay(amount);
        System.out.println("✅ Payment processed successfully\n");
    }
}

// Usage
public class StrategyPatternDemo {
    public static void main(String[] args) {
        PaymentContext paymentContext = new PaymentContext();
        
        // Pay with Credit Card
        PaymentStrategy creditCard = new CreditCardPayment("1234567890123456", "John Doe", "12/25", "123");
        paymentContext.setPaymentStrategy(creditCard);
        paymentContext.processPayment(100.0);
        
        // Pay with PayPal
        PaymentStrategy paypal = new PayPalPayment("john.doe@example.com", "password123");
        paymentContext.setPaymentStrategy(paypal);
        paymentContext.processPayment(250.0);
        
        // Pay with Bank Transfer
        PaymentStrategy bankTransfer = new BankTransferPayment("12345678901", "123456789", "Wells Fargo");
        paymentContext.setPaymentStrategy(bankTransfer);
        paymentContext.processPayment(500.0);
        
        // Try invalid payment
        PaymentStrategy invalidCard = new CreditCardPayment("123", "Jane Doe", "12/25", "12");
        paymentContext.setPaymentStrategy(invalidCard);
        paymentContext.processPayment(75.0);
    }
}
```

### Example 2: Sorting Algorithms

```java
import java.util.*;

// Strategy interface
interface SortingStrategy {
    void sort(int[] array);
    String getName();
}

// Concrete Strategy - Bubble Sort
class BubbleSort implements SortingStrategy {
    @Override
    public void sort(int[] array) {
        System.out.println("🔄 Performing Bubble Sort...");
        int n = array.length;
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                if (array[j] > array[j + 1]) {
                    // Swap elements
                    int temp = array[j];
                    array[j] = array[j + 1];
                    array[j + 1] = temp;
                }
            }
        }
    }
    
    @Override
    public String getName() {
        return "Bubble Sort";
    }
}

// Concrete Strategy - Quick Sort
class QuickSort implements SortingStrategy {
    @Override
    public void sort(int[] array) {
        System.out.println("⚡ Performing Quick Sort...");
        quickSort(array, 0, array.length - 1);
    }
    
    private void quickSort(int[] array, int low, int high) {
        if (low < high) {
            int pi = partition(array, low, high);
            quickSort(array, low, pi - 1);
            quickSort(array, pi + 1, high);
        }
    }
    
    private int partition(int[] array, int low, int high) {
        int pivot = array[high];
        int i = (low - 1);
        
        for (int j = low; j < high; j++) {
            if (array[j] <= pivot) {
                i++;
                int temp = array[i];
                array[i] = array[j];
                array[j] = temp;
            }
        }
        
        int temp = array[i + 1];
        array[i + 1] = array[high];
        array[high] = temp;
        
        return i + 1;
    }
    
    @Override
    public String getName() {
        return "Quick Sort";
    }
}

// Concrete Strategy - Merge Sort
class MergeSort implements SortingStrategy {
    @Override
    public void sort(int[] array) {
        System.out.println("🔀 Performing Merge Sort...");
        mergeSort(array, 0, array.length - 1);
    }
    
    private void mergeSort(int[] array, int left, int right) {
        if (left < right) {
            int mid = (left + right) / 2;
            mergeSort(array, left, mid);
            mergeSort(array, mid + 1, right);
            merge(array, left, mid, right);
        }
    }
    
    private void merge(int[] array, int left, int mid, int right) {
        int n1 = mid - left + 1;
        int n2 = right - mid;
        
        int[] leftArray = new int[n1];
        int[] rightArray = new int[n2];
        
        System.arraycopy(array, left, leftArray, 0, n1);
        System.arraycopy(array, mid + 1, rightArray, 0, n2);
        
        int i = 0, j = 0, k = left;
        
        while (i < n1 && j < n2) {
            if (leftArray[i] <= rightArray[j]) {
                array[k] = leftArray[i];
                i++;
            } else {
                array[k] = rightArray[j];
                j++;
            }
            k++;
        }
        
        while (i < n1) {
            array[k] = leftArray[i];
            i++;
            k++;
        }
        
        while (j < n2) {
            array[k] = rightArray[j];
            j++;
            k++;
        }
    }
    
    @Override
    public String getName() {
        return "Merge Sort";
    }
}

// Context class
class SortingContext {
    private SortingStrategy sortingStrategy;
    
    public void setSortingStrategy(SortingStrategy sortingStrategy) {
        this.sortingStrategy = sortingStrategy;
    }
    
    public void performSort(int[] array) {
        if (sortingStrategy == null) {
            System.out.println("❌ No sorting strategy selected");
            return;
        }
        
        System.out.println("Original array: " + Arrays.toString(array));
        
        long startTime = System.nanoTime();
        sortingStrategy.sort(array);
        long endTime = System.nanoTime();
        
        System.out.println("Sorted array: " + Arrays.toString(array));
        System.out.println("Time taken: " + (endTime - startTime) / 1000000.0 + " ms");
        System.out.println("Algorithm: " + sortingStrategy.getName());
        System.out.println("=".repeat(50));
    }
}

// Usage
public class SortingStrategyDemo {
    public static void main(String[] args) {
        SortingContext context = new SortingContext();
        
        // Test data
        int[] data1 = {64, 34, 25, 12, 22, 11, 90};
        int[] data2 = {64, 34, 25, 12, 22, 11, 90};
        int[] data3 = {64, 34, 25, 12, 22, 11, 90};
        
        // Use Bubble Sort
        context.setSortingStrategy(new BubbleSort());
        context.performSort(data1);
        
        // Use Quick Sort
        context.setSortingStrategy(new QuickSort());
        context.performSort(data2);
        
        // Use Merge Sort
        context.setSortingStrategy(new MergeSort());
        context.performSort(data3);
    }
}
```

---

## 3. Practical Exercises

### Exercise 1: Weather Monitoring System with Observer Pattern

```java
import java.util.*;

// Weather data structure
class WeatherData {
    private double temperature;
    private double humidity;
    private double pressure;
    
    public WeatherData(double temperature, double humidity, double pressure) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.pressure = pressure;
    }
    
    // Getters
    public double getTemperature() { return temperature; }
    public double getHumidity() { return humidity; }
    public double getPressure() { return pressure; }
    
    @Override
    public String toString() {
        return String.format("Temp: %.1f°F, Humidity: %.1f%%, Pressure: %.1f", 
                           temperature, humidity, pressure);
    }
}

// Observer interface
interface WeatherObserver {
    void update(WeatherData weatherData);
}

// Weather Station (Subject)
class WeatherStation {
    private List<WeatherObserver> observers = new ArrayList<>();
    private WeatherData currentWeather;
    
    public void addObserver(WeatherObserver observer) {
        observers.add(observer);
    }
    
    public void removeObserver(WeatherObserver observer) {
        observers.remove(observer);
    }
    
    public void notifyObservers() {
        for (WeatherObserver observer : observers) {
            observer.update(currentWeather);
        }
    }
    
    public void setWeatherData(double temperature, double humidity, double pressure) {
        this.currentWeather = new WeatherData(temperature, humidity, pressure);
        System.out.println("🌤️ Weather Station: New data received - " + currentWeather);
        notifyObservers();
    }
}

// Concrete Observer - Current Conditions Display
class CurrentConditionsDisplay implements WeatherObserver {
    @Override
    public void update(WeatherData weatherData) {
        System.out.println("📊 Current Conditions: " + weatherData);
    }
}

// Concrete Observer - Statistics Display
class StatisticsDisplay implements WeatherObserver {
    private List<Double> temperatures = new ArrayList<>();
    
    @Override
    public void update(WeatherData weatherData) {
        temperatures.add(weatherData.getTemperature());
        double avg = temperatures.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double min = temperatures.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        double max = temperatures.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        
        System.out.printf("📈 Statistics: Avg: %.1f°F, Min: %.1f°F, Max: %.1f°F%n", avg, min, max);
    }
}

// Concrete Observer - Forecast Display
class ForecastDisplay implements WeatherObserver {
    private double lastPressure = 0.0;
    
    @Override
    public void update(WeatherData weatherData) {
        double currentPressure = weatherData.getPressure();
        String forecast;
        
        if (currentPressure > lastPressure) {
            forecast = "Improving weather on the way!";
        } else if (currentPressure < lastPressure) {
            forecast = "Watch out for cooler, rainy weather";
        } else {
            forecast = "More of the same";
        }
        
        System.out.println("🔮 Forecast: " + forecast);
        lastPressure = currentPressure;
    }
}

// Usage
public class WeatherMonitoringDemo {
    public static void main(String[] args) {
        WeatherStation station = new WeatherStation();
        
        // Create displays
        CurrentConditionsDisplay currentDisplay = new CurrentConditionsDisplay();
        StatisticsDisplay statisticsDisplay = new StatisticsDisplay();
        ForecastDisplay forecastDisplay = new ForecastDisplay();
        
        // Add observers
        station.addObserver(currentDisplay);
        station.addObserver(statisticsDisplay);
        station.addObserver(forecastDisplay);
        
        // Simulate weather updates
        System.out.println("=== Weather Updates ===");
        station.setWeatherData(80.0, 65.0, 30.4);
        System.out.println();
        
        station.setWeatherData(82.0, 70.0, 29.2);
        System.out.println();
        
        station.setWeatherData(78.0, 90.0, 29.2);
        System.out.println();
        
        station.setWeatherData(75.0, 85.0, 30.1);
    }
}
```

### Exercise 2: Discount Strategy System

```java
// Strategy interface
interface DiscountStrategy {
    double calculateDiscount(double originalPrice);
    String getDescription();
}

// Concrete Strategy - No Discount
class NoDiscountStrategy implements DiscountStrategy {
    @Override
    public double calculateDiscount(double originalPrice) {
        return originalPrice;
    }
    
    @Override
    public String getDescription() {
        return "No discount applied";
    }
}

// Concrete Strategy - Percentage Discount
class PercentageDiscountStrategy implements DiscountStrategy {
    private double percentage;
    
    public PercentageDiscountStrategy(double percentage) {
        this.percentage = percentage;
    }
    
    @Override
    public double calculateDiscount(double originalPrice) {
        return originalPrice * (1 - percentage / 100);
    }
    
    @Override
    public String getDescription() {
        return percentage + "% discount applied";
    }
}

// Concrete Strategy - Fixed Amount Discount
class FixedAmountDiscountStrategy implements DiscountStrategy {
    private double discountAmount;
    
    public FixedAmountDiscountStrategy(double discountAmount) {
        this.discountAmount = discountAmount;
    }
    
    @Override
    public double calculateDiscount(double originalPrice) {
        return Math.max(0, originalPrice - discountAmount);
    }
    
    @Override
    public String getDescription() {
        return "$" + discountAmount + " discount applied";
    }
}

// Concrete Strategy - Buy One Get One Free
class BOGODiscountStrategy implements DiscountStrategy {
    @Override
    public double calculateDiscount(double originalPrice) {
        return originalPrice * 0.5; // 50% discount for BOGO
    }
    
    @Override
    public String getDescription() {
        return "Buy One Get One Free (50% off)";
    }
}

// Context class
class ShoppingCart {
    private List<String> items = new ArrayList<>();
    private List<Double> prices = new ArrayList<>();
    private DiscountStrategy discountStrategy;
    
    public void addItem(String item, double price) {
        items.add(item);
        prices.add(price);
    }
    
    public void setDiscountStrategy(DiscountStrategy discountStrategy) {
        this.discountStrategy = discountStrategy;
    }
    
    public void checkout() {
        double totalOriginalPrice = prices.stream().mapToDouble(Double::doubleValue).sum();
        
        System.out.println("🛒 Shopping Cart:");
        for (int i = 0; i < items.size(); i++) {
            System.out.println("   " + items.get(i) + " - $" + prices.get(i));
        }
        
        System.out.println("💰 Original Total: $" + totalOriginalPrice);
        
        if (discountStrategy != null) {
            double finalPrice = discountStrategy.calculateDiscount(totalOriginalPrice);
            double savings = totalOriginalPrice - finalPrice;
            
            System.out.println("🎯 " + discountStrategy.getDescription());
            System.out.println("💵 Final Price: $" + finalPrice);
            System.out.println("🎉 You saved: $" + savings);
        } else {
            System.out.println("💵 Final Price: $" + totalOriginalPrice);
        }
        
        System.out.println("=".repeat(40));
    }
}

// Usage
public class DiscountStrategyDemo {
    public static void main(String[] args) {
        ShoppingCart cart = new ShoppingCart();
        
        // Add items to cart
        cart.addItem("Laptop", 999.99);
        cart.addItem("Mouse", 29.99);
        cart.addItem("Keyboard", 79.99);
        
        // No discount
        cart.setDiscountStrategy(new NoDiscountStrategy());
        cart.checkout();
        
        // 10% discount
        cart.setDiscountStrategy(new PercentageDiscountStrategy(10));
        cart.checkout();
        
        // $50 off
        cart.setDiscountStrategy(new FixedAmountDiscountStrategy(50));
        cart.checkout();
        
        // BOGO discount
        cart.setDiscountStrategy(new BOGODiscountStrategy());
        cart.checkout();
    }
}
```

---

## 4. Key Takeaways

### Observer Pattern Benefits:
1. **Loose Coupling**: Subject and observers are loosely coupled
2. **Dynamic Relationships**: Observers can be added/removed at runtime
3. **Broadcast Communication**: One-to-many communication mechanism
4. **Open/Closed Principle**: Easy to add new observer types

### Strategy Pattern Benefits:
1. **Algorithm Flexibility**: Algorithms can be switched at runtime
2. **Code Reusability**: Strategies can be reused across different contexts
3. **Testability**: Each strategy can be tested independently
4. **Maintainability**: Easy to add new algorithms without modifying existing code

### When to Use:
- **Observer**: When changes to one object require updating multiple dependent objects
- **Strategy**: When you have multiple ways to perform a task and want to choose at runtime

### Best Practices:
1. **Observer**: Consider using weak references to prevent memory leaks
2. **Strategy**: Use factory pattern to create strategies
3. **Both**: Follow interface segregation principle
4. **Both**: Consider thread safety in multi-threaded environments

---

## 5. Next Steps

Tomorrow we'll explore more **Behavioral Patterns** including Command and State patterns, which help us encapsulate requests and manage object state changes.

**Practice Assignment**: Create a notification system using Observer pattern and a file compression utility using Strategy pattern with different compression algorithms.

---

*"The Observer pattern is like a newsletter subscription - when news breaks, all subscribers get notified automatically."*

*"The Strategy pattern is like having different routes to the same destination - you can choose the best one based on current conditions."*