# Day 53: Structural Patterns - Facade & Composite

## Learning Objectives
- Master the Facade pattern for simplifying complex subsystems
- Understand the Composite pattern for tree structures
- Learn when and how to apply these patterns
- Build practical examples with real-world scenarios

---

## 1. Facade Pattern

The Facade pattern provides a unified interface to a set of interfaces in a subsystem. It defines a higher-level interface that makes the subsystem easier to use.

### Key Concepts:
- **Simplified Interface**: Hides complexity of subsystem
- **Decoupling**: Clients don't need to know about subsystem classes
- **Convenience**: Provides convenient methods for common operations

### Example 1: Home Theater System

```java
// Complex subsystem classes
class DVDPlayer {
    public void on() { System.out.println("DVD Player on"); }
    public void off() { System.out.println("DVD Player off"); }
    public void play(String movie) { System.out.println("Playing: " + movie); }
    public void stop() { System.out.println("DVD Player stopped"); }
}

class Amplifier {
    public void on() { System.out.println("Amplifier on"); }
    public void off() { System.out.println("Amplifier off"); }
    public void setVolume(int level) { System.out.println("Volume set to: " + level); }
}

class Projector {
    public void on() { System.out.println("Projector on"); }
    public void off() { System.out.println("Projector off"); }
    public void setInput(String input) { System.out.println("Input set to: " + input); }
}

class TheaterLights {
    public void dim(int level) { System.out.println("Lights dimmed to: " + level + "%"); }
    public void on() { System.out.println("Lights on"); }
}

class Screen {
    public void down() { System.out.println("Screen down"); }
    public void up() { System.out.println("Screen up"); }
}

// Facade class
class HomeTheaterFacade {
    private DVDPlayer dvdPlayer;
    private Amplifier amplifier;
    private Projector projector;
    private TheaterLights lights;
    private Screen screen;
    
    public HomeTheaterFacade(DVDPlayer dvdPlayer, Amplifier amplifier, 
                           Projector projector, TheaterLights lights, Screen screen) {
        this.dvdPlayer = dvdPlayer;
        this.amplifier = amplifier;
        this.projector = projector;
        this.lights = lights;
        this.screen = screen;
    }
    
    public void watchMovie(String movie) {
        System.out.println("Get ready to watch a movie...");
        lights.dim(10);
        screen.down();
        projector.on();
        projector.setInput("DVD");
        amplifier.on();
        amplifier.setVolume(5);
        dvdPlayer.on();
        dvdPlayer.play(movie);
        System.out.println("Movie started!\n");
    }
    
    public void endMovie() {
        System.out.println("Shutting down movie theater...");
        dvdPlayer.stop();
        dvdPlayer.off();
        amplifier.off();
        projector.off();
        screen.up();
        lights.on();
        System.out.println("Movie theater shut down!\n");
    }
}

// Usage
public class FacadePatternDemo {
    public static void main(String[] args) {
        // Create subsystem objects
        DVDPlayer dvdPlayer = new DVDPlayer();
        Amplifier amplifier = new Amplifier();
        Projector projector = new Projector();
        TheaterLights lights = new TheaterLights();
        Screen screen = new Screen();
        
        // Create facade
        HomeTheaterFacade homeTheater = new HomeTheaterFacade(
            dvdPlayer, amplifier, projector, lights, screen
        );
        
        // Use facade for simple operations
        homeTheater.watchMovie("The Matrix");
        homeTheater.endMovie();
    }
}
```

### Example 2: Computer System Facade

```java
// Complex subsystem classes
class CPU {
    public void freeze() { System.out.println("CPU: Freezing processor"); }
    public void jump(long position) { System.out.println("CPU: Jumping to position " + position); }
    public void execute() { System.out.println("CPU: Executing instructions"); }
}

class Memory {
    public void load(long position, byte[] data) {
        System.out.println("Memory: Loading " + data.length + " bytes at position " + position);
    }
}

class HardDrive {
    public byte[] read(long lba, int size) {
        System.out.println("HardDrive: Reading " + size + " bytes from LBA " + lba);
        return new byte[size];
    }
}

// Facade class
class ComputerFacade {
    private CPU cpu;
    private Memory memory;
    private HardDrive hardDrive;
    
    public ComputerFacade() {
        this.cpu = new CPU();
        this.memory = new Memory();
        this.hardDrive = new HardDrive();
    }
    
    public void start() {
        System.out.println("Starting computer...");
        cpu.freeze();
        memory.load(0, hardDrive.read(0, 1024));
        cpu.jump(0);
        cpu.execute();
        System.out.println("Computer started successfully!\n");
    }
}

// Usage
public class ComputerFacadeDemo {
    public static void main(String[] args) {
        ComputerFacade computer = new ComputerFacade();
        computer.start();
    }
}
```

---

## 2. Composite Pattern

The Composite pattern composes objects into tree structures to represent part-whole hierarchies. It lets clients treat individual objects and compositions of objects uniformly.

### Key Concepts:
- **Component**: Base interface for all objects
- **Leaf**: Individual objects with no children
- **Composite**: Objects that can contain other components
- **Uniform Treatment**: Same interface for leaves and composites

### Example 1: File System

```java
// Component interface
abstract class FileSystemComponent {
    protected String name;
    
    public FileSystemComponent(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    // Default implementations
    public void add(FileSystemComponent component) {
        throw new UnsupportedOperationException("Cannot add to a file");
    }
    
    public void remove(FileSystemComponent component) {
        throw new UnsupportedOperationException("Cannot remove from a file");
    }
    
    public FileSystemComponent getChild(int index) {
        throw new UnsupportedOperationException("Cannot get child of a file");
    }
    
    public abstract void display(int depth);
    public abstract long getSize();
}

// Leaf class - File
class File extends FileSystemComponent {
    private long size;
    
    public File(String name, long size) {
        super(name);
        this.size = size;
    }
    
    @Override
    public void display(int depth) {
        System.out.println("  ".repeat(depth) + "📄 " + name + " (" + size + " bytes)");
    }
    
    @Override
    public long getSize() {
        return size;
    }
}

// Composite class - Directory
class Directory extends FileSystemComponent {
    private List<FileSystemComponent> children = new ArrayList<>();
    
    public Directory(String name) {
        super(name);
    }
    
    @Override
    public void add(FileSystemComponent component) {
        children.add(component);
    }
    
    @Override
    public void remove(FileSystemComponent component) {
        children.remove(component);
    }
    
    @Override
    public FileSystemComponent getChild(int index) {
        return children.get(index);
    }
    
    @Override
    public void display(int depth) {
        System.out.println("  ".repeat(depth) + "📁 " + name + "/");
        for (FileSystemComponent child : children) {
            child.display(depth + 1);
        }
    }
    
    @Override
    public long getSize() {
        long totalSize = 0;
        for (FileSystemComponent child : children) {
            totalSize += child.getSize();
        }
        return totalSize;
    }
    
    public int getFileCount() {
        int count = 0;
        for (FileSystemComponent child : children) {
            if (child instanceof File) {
                count++;
            } else if (child instanceof Directory) {
                count += ((Directory) child).getFileCount();
            }
        }
        return count;
    }
}

// Usage
public class CompositePatternDemo {
    public static void main(String[] args) {
        // Create root directory
        Directory root = new Directory("root");
        
        // Create subdirectories
        Directory documents = new Directory("documents");
        Directory photos = new Directory("photos");
        Directory music = new Directory("music");
        
        // Create files
        File resume = new File("resume.pdf", 2048);
        File photo1 = new File("vacation.jpg", 1024000);
        File photo2 = new File("family.png", 2048000);
        File song1 = new File("song.mp3", 4096000);
        File song2 = new File("classical.mp3", 3072000);
        
        // Build file system structure
        root.add(documents);
        root.add(photos);
        root.add(music);
        
        documents.add(resume);
        photos.add(photo1);
        photos.add(photo2);
        music.add(song1);
        music.add(song2);
        
        // Display file system
        System.out.println("File System Structure:");
        root.display(0);
        
        System.out.println("\nTotal size: " + root.getSize() + " bytes");
        System.out.println("Total files: " + root.getFileCount());
    }
}
```

### Example 2: Organization Structure

```java
// Component interface
abstract class OrganizationComponent {
    protected String name;
    protected String position;
    protected double salary;
    
    public OrganizationComponent(String name, String position, double salary) {
        this.name = name;
        this.position = position;
        this.salary = salary;
    }
    
    public String getName() { return name; }
    public String getPosition() { return position; }
    public double getSalary() { return salary; }
    
    // Default implementations
    public void add(OrganizationComponent component) {
        throw new UnsupportedOperationException("Cannot add subordinates to this position");
    }
    
    public void remove(OrganizationComponent component) {
        throw new UnsupportedOperationException("Cannot remove subordinates from this position");
    }
    
    public abstract void display(int depth);
    public abstract double getTotalSalary();
    public abstract int getEmployeeCount();
}

// Leaf class - Employee
class Employee extends OrganizationComponent {
    public Employee(String name, String position, double salary) {
        super(name, position, salary);
    }
    
    @Override
    public void display(int depth) {
        System.out.println("  ".repeat(depth) + "👤 " + name + " (" + position + ") - $" + salary);
    }
    
    @Override
    public double getTotalSalary() {
        return salary;
    }
    
    @Override
    public int getEmployeeCount() {
        return 1;
    }
}

// Composite class - Manager
class Manager extends OrganizationComponent {
    private List<OrganizationComponent> subordinates = new ArrayList<>();
    
    public Manager(String name, String position, double salary) {
        super(name, position, salary);
    }
    
    @Override
    public void add(OrganizationComponent component) {
        subordinates.add(component);
    }
    
    @Override
    public void remove(OrganizationComponent component) {
        subordinates.remove(component);
    }
    
    @Override
    public void display(int depth) {
        System.out.println("  ".repeat(depth) + "👔 " + name + " (" + position + ") - $" + salary);
        for (OrganizationComponent subordinate : subordinates) {
            subordinate.display(depth + 1);
        }
    }
    
    @Override
    public double getTotalSalary() {
        double totalSalary = salary;
        for (OrganizationComponent subordinate : subordinates) {
            totalSalary += subordinate.getTotalSalary();
        }
        return totalSalary;
    }
    
    @Override
    public int getEmployeeCount() {
        int count = 1; // Include this manager
        for (OrganizationComponent subordinate : subordinates) {
            count += subordinate.getEmployeeCount();
        }
        return count;
    }
}

// Usage
public class OrganizationCompositeDemo {
    public static void main(String[] args) {
        // Create CEO
        Manager ceo = new Manager("John Smith", "CEO", 200000);
        
        // Create department heads
        Manager cto = new Manager("Jane Doe", "CTO", 150000);
        Manager cfo = new Manager("Bob Johnson", "CFO", 140000);
        
        // Create team leads
        Manager devLead = new Manager("Alice Wilson", "Dev Lead", 120000);
        Manager qaLead = new Manager("Charlie Brown", "QA Lead", 110000);
        
        // Create employees
        Employee dev1 = new Employee("David Lee", "Senior Developer", 90000);
        Employee dev2 = new Employee("Emma Davis", "Junior Developer", 70000);
        Employee qa1 = new Employee("Frank Miller", "QA Engineer", 80000);
        Employee accountant = new Employee("Grace Taylor", "Accountant", 75000);
        
        // Build organization structure
        ceo.add(cto);
        ceo.add(cfo);
        
        cto.add(devLead);
        cto.add(qaLead);
        
        devLead.add(dev1);
        devLead.add(dev2);
        qaLead.add(qa1);
        
        cfo.add(accountant);
        
        // Display organization
        System.out.println("Organization Structure:");
        ceo.display(0);
        
        System.out.println("\nTotal employees: " + ceo.getEmployeeCount());
        System.out.println("Total salary cost: $" + ceo.getTotalSalary());
    }
}
```

---

## 3. Practical Exercises

### Exercise 1: Banking System Facade
Create a facade for a banking system that simplifies complex operations:

```java
// Complex subsystem classes
class AccountService {
    public boolean validateAccount(String accountNumber) {
        System.out.println("Validating account: " + accountNumber);
        return true;
    }
    
    public double getBalance(String accountNumber) {
        System.out.println("Getting balance for: " + accountNumber);
        return 1000.0;
    }
    
    public void debit(String accountNumber, double amount) {
        System.out.println("Debiting $" + amount + " from " + accountNumber);
    }
    
    public void credit(String accountNumber, double amount) {
        System.out.println("Crediting $" + amount + " to " + accountNumber);
    }
}

class SecurityService {
    public boolean authenticate(String pin) {
        System.out.println("Authenticating with PIN: " + pin);
        return true;
    }
    
    public boolean authorize(String operation) {
        System.out.println("Authorizing operation: " + operation);
        return true;
    }
}

class NotificationService {
    public void sendSMS(String message) {
        System.out.println("SMS: " + message);
    }
    
    public void sendEmail(String message) {
        System.out.println("Email: " + message);
    }
}

class AuditService {
    public void logTransaction(String details) {
        System.out.println("Audit Log: " + details);
    }
}

// Facade class
class BankingFacade {
    private AccountService accountService;
    private SecurityService securityService;
    private NotificationService notificationService;
    private AuditService auditService;
    
    public BankingFacade() {
        this.accountService = new AccountService();
        this.securityService = new SecurityService();
        this.notificationService = new NotificationService();
        this.auditService = new AuditService();
    }
    
    public boolean transfer(String fromAccount, String toAccount, double amount, String pin) {
        System.out.println("\nProcessing transfer...");
        
        // Step 1: Authenticate
        if (!securityService.authenticate(pin)) {
            System.out.println("Authentication failed!");
            return false;
        }
        
        // Step 2: Validate accounts
        if (!accountService.validateAccount(fromAccount) || !accountService.validateAccount(toAccount)) {
            System.out.println("Invalid account(s)!");
            return false;
        }
        
        // Step 3: Check authorization
        if (!securityService.authorize("TRANSFER")) {
            System.out.println("Transfer not authorized!");
            return false;
        }
        
        // Step 4: Check balance
        if (accountService.getBalance(fromAccount) < amount) {
            System.out.println("Insufficient funds!");
            return false;
        }
        
        // Step 5: Perform transfer
        accountService.debit(fromAccount, amount);
        accountService.credit(toAccount, amount);
        
        // Step 6: Send notifications
        notificationService.sendSMS("Transfer of $" + amount + " completed");
        notificationService.sendEmail("Transfer confirmation: $" + amount);
        
        // Step 7: Log transaction
        auditService.logTransaction("Transfer: " + fromAccount + " -> " + toAccount + ", Amount: $" + amount);
        
        System.out.println("Transfer completed successfully!");
        return true;
    }
}

// Usage
public class BankingFacadeDemo {
    public static void main(String[] args) {
        BankingFacade banking = new BankingFacade();
        banking.transfer("12345", "67890", 500.0, "1234");
    }
}
```

### Exercise 2: Menu System Composite
Create a composite pattern for a restaurant menu system:

```java
// Component interface
abstract class MenuComponent {
    protected String name;
    protected String description;
    protected double price;
    
    public MenuComponent(String name, String description, double price) {
        this.name = name;
        this.description = description;
        this.price = price;
    }
    
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    
    // Default implementations
    public void add(MenuComponent component) {
        throw new UnsupportedOperationException("Cannot add to menu item");
    }
    
    public void remove(MenuComponent component) {
        throw new UnsupportedOperationException("Cannot remove from menu item");
    }
    
    public abstract void display(int depth);
    public abstract double getTotalPrice();
}

// Leaf class - Menu Item
class MenuItem extends MenuComponent {
    private boolean vegetarian;
    
    public MenuItem(String name, String description, double price, boolean vegetarian) {
        super(name, description, price);
        this.vegetarian = vegetarian;
    }
    
    @Override
    public void display(int depth) {
        String indent = "  ".repeat(depth);
        String vegLabel = vegetarian ? "🌱" : "🍖";
        System.out.println(indent + vegLabel + " " + name + " - $" + price);
        System.out.println(indent + "   " + description);
    }
    
    @Override
    public double getTotalPrice() {
        return price;
    }
}

// Composite class - Menu
class Menu extends MenuComponent {
    private List<MenuComponent> menuComponents = new ArrayList<>();
    
    public Menu(String name, String description) {
        super(name, description, 0.0);
    }
    
    @Override
    public void add(MenuComponent component) {
        menuComponents.add(component);
    }
    
    @Override
    public void remove(MenuComponent component) {
        menuComponents.remove(component);
    }
    
    @Override
    public void display(int depth) {
        String indent = "  ".repeat(depth);
        System.out.println(indent + "📋 " + name.toUpperCase());
        System.out.println(indent + "   " + description);
        System.out.println(indent + "   " + "-".repeat(30));
        
        for (MenuComponent component : menuComponents) {
            component.display(depth + 1);
        }
    }
    
    @Override
    public double getTotalPrice() {
        double total = 0.0;
        for (MenuComponent component : menuComponents) {
            total += component.getTotalPrice();
        }
        return total;
    }
}

// Usage
public class MenuCompositeDemo {
    public static void main(String[] args) {
        // Create main menu
        Menu restaurantMenu = new Menu("Restaurant Menu", "Welcome to our restaurant!");
        
        // Create sub-menus
        Menu breakfast = new Menu("Breakfast Menu", "Served until 11:00 AM");
        Menu lunch = new Menu("Lunch Menu", "Served 11:00 AM - 3:00 PM");
        Menu dinner = new Menu("Dinner Menu", "Served 5:00 PM - 10:00 PM");
        Menu desserts = new Menu("Dessert Menu", "Sweet endings");
        
        // Add breakfast items
        breakfast.add(new MenuItem("Pancakes", "Fluffy pancakes with maple syrup", 8.99, true));
        breakfast.add(new MenuItem("Eggs Benedict", "Poached eggs with hollandaise sauce", 12.99, false));
        breakfast.add(new MenuItem("Fruit Bowl", "Fresh seasonal fruits", 6.99, true));
        
        // Add lunch items
        lunch.add(new MenuItem("Caesar Salad", "Crisp romaine with parmesan and croutons", 9.99, false));
        lunch.add(new MenuItem("Grilled Chicken Sandwich", "With avocado and bacon", 13.99, false));
        lunch.add(new MenuItem("Vegetable Wrap", "Fresh vegetables in a spinach tortilla", 10.99, true));
        
        // Add dinner items
        dinner.add(new MenuItem("Grilled Salmon", "Atlantic salmon with lemon butter", 22.99, false));
        dinner.add(new MenuItem("Ribeye Steak", "12oz ribeye with garlic mashed potatoes", 28.99, false));
        dinner.add(new MenuItem("Vegetable Pasta", "Penne with seasonal vegetables", 16.99, true));
        
        // Add desserts
        desserts.add(new MenuItem("Chocolate Cake", "Rich chocolate cake with vanilla ice cream", 7.99, true));
        desserts.add(new MenuItem("Tiramisu", "Classic Italian dessert", 6.99, true));
        
        // Build menu structure
        restaurantMenu.add(breakfast);
        restaurantMenu.add(lunch);
        restaurantMenu.add(dinner);
        restaurantMenu.add(desserts);
        
        // Display menu
        restaurantMenu.display(0);
        
        System.out.println("\n" + "=".repeat(50));
        System.out.println("Total menu value: $" + restaurantMenu.getTotalPrice());
    }
}
```

---

## 4. Key Takeaways

### Facade Pattern Benefits:
1. **Simplified Interface**: Provides easy-to-use interface for complex subsystems
2. **Reduced Coupling**: Clients don't depend on subsystem classes
3. **Layered Architecture**: Promotes clean separation of concerns
4. **Flexibility**: Subsystems can change without affecting clients

### Composite Pattern Benefits:
1. **Uniform Treatment**: Same interface for individual and composite objects
2. **Recursive Composition**: Can create complex tree structures
3. **Extensibility**: Easy to add new component types
4. **Simplified Client Code**: Clients don't need to distinguish between leaves and composites

### When to Use:
- **Facade**: When you need to provide a simple interface to a complex subsystem
- **Composite**: When you need to represent part-whole hierarchies and treat objects uniformly

### Best Practices:
1. **Facade**: Don't expose all subsystem functionality - only what clients need
2. **Composite**: Use abstract base class or interface for type safety
3. **Both**: Follow Single Responsibility Principle
4. **Both**: Consider performance implications of recursive operations

---

## 5. Next Steps

Tomorrow we'll explore **Behavioral Patterns** including Observer and Strategy patterns, which focus on communication between objects and encapsulating algorithms.

**Practice Assignment**: Create a GUI application facade that simplifies complex UI operations, and use the composite pattern to represent a hierarchical menu system.

---

*"The Facade pattern is like a helpful receptionist who knows how to get things done without you having to understand the complex organization behind the scenes."*

*"The Composite pattern is like a family tree where you can treat individuals and families the same way - asking for information or performing operations uniformly."*