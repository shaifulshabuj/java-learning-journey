# Day 7: Inheritance - Building Class Hierarchies
**Date:** July 16, 2025  
**Duration:** 1.5 hours (Combined Session)  
**Focus:** extends keyword, method overriding, super keyword, and Object class methods

---

## 🎯 Today's Learning Goals
By the end of this session, you'll:
- ✅ Create class hierarchies using the `extends` keyword
- ✅ Master method overriding vs method overloading
- ✅ Use the `super` keyword for parent class access
- ✅ Override Object class methods (toString, equals, hashCode)
- ✅ Understand constructor chaining in inheritance
- ✅ Apply the `final` keyword to classes and methods
- ✅ Build a complete inheritance-based application

---

## 📚 Part 1: Basic Inheritance (25 minutes)

### **Understanding IS-A Relationships**

Create `Animal.java` (Base/Parent Class):

```java
/**
 * Base Animal class - demonstrates inheritance fundamentals
 */
public class Animal {
    
    // Protected fields - accessible to subclasses
    protected String name;
    protected int age;
    protected String species;
    protected double weight;
    protected boolean isAlive;
    
    // Default constructor
    public Animal() {
        this.name = "Unknown";
        this.age = 0;
        this.species = "Unknown";
        this.weight = 0.0;
        this.isAlive = true;
        System.out.println("Animal default constructor called");
    }
    
    // Parameterized constructor
    public Animal(String name, int age, String species, double weight) {
        this.name = name;
        this.age = age;
        this.species = species;
        this.weight = weight;
        this.isAlive = true;
        System.out.println("Animal parameterized constructor called for: " + name);
    }
    
    // Methods that can be inherited
    public void eat() {
        System.out.println(this.name + " is eating");
    }
    
    public void sleep() {
        System.out.println(this.name + " is sleeping");
    }
    
    public void breathe() {
        System.out.println(this.name + " is breathing");
    }
    
    // Method that will be overridden by subclasses
    public void makeSound() {
        System.out.println(this.name + " makes a generic animal sound");
    }
    
    // Method that will be overridden
    public void move() {
        System.out.println(this.name + " is moving");
    }
    
    // Final method - cannot be overridden
    public final void age() {
        this.age++;
        System.out.println(this.name + " is now " + this.age + " years old");
    }
    
    // Getters and setters
    public String getName() { return this.name; }
    public void setName(String name) { this.name = name; }
    
    public int getAge() { return this.age; }
    public void setAge(int age) { 
        if (age >= 0) this.age = age; 
    }
    
    public String getSpecies() { return this.species; }
    public double getWeight() { return this.weight; }
    public boolean isAlive() { return this.isAlive; }
    
    // Display information
    public void displayInfo() {
        System.out.println("=== Animal Information ===");
        System.out.println("Name: " + this.name);
        System.out.println("Age: " + this.age + " years");
        System.out.println("Species: " + this.species);
        System.out.println("Weight: " + this.weight + " kg");
        System.out.println("Status: " + (this.isAlive ? "Alive" : "Deceased"));
        System.out.println();
    }
    
    // Override Object class methods
    @Override
    public String toString() {
        return String.format("Animal{name='%s', age=%d, species='%s', weight=%.1f}", 
                           name, age, species, weight);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Animal animal = (Animal) obj;
        return age == animal.age &&
               Double.compare(animal.weight, weight) == 0 &&
               isAlive == animal.isAlive &&
               name.equals(animal.name) &&
               species.equals(animal.species);
    }
    
    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + age;
        result = 31 * result + species.hashCode();
        result = 31 * result + Double.hashCode(weight);
        result = 31 * result + Boolean.hashCode(isAlive);
        return result;
    }
}
```

### **Creating Subclasses**

Create `Mammal.java` (Intermediate Class):

```java
/**
 * Mammal class extends Animal - demonstrates inheritance chain
 */
public class Mammal extends Animal {
    
    // Additional properties specific to mammals
    protected String furColor;
    protected boolean isWarmBlooded;
    protected int gestationPeriod; // in days
    
    // Default constructor
    public Mammal() {
        super(); // Call parent constructor
        this.furColor = "Brown";
        this.isWarmBlooded = true;
        this.gestationPeriod = 30;
        System.out.println("Mammal default constructor called");
    }
    
    // Parameterized constructor
    public Mammal(String name, int age, String species, double weight, 
                  String furColor, int gestationPeriod) {
        super(name, age, species, weight); // Call parent constructor
        this.furColor = furColor;
        this.isWarmBlooded = true;
        this.gestationPeriod = gestationPeriod;
        System.out.println("Mammal parameterized constructor called for: " + name);
    }
    
    // New methods specific to mammals
    public void regulateBodyTemperature() {
        System.out.println(this.name + " is regulating body temperature (warm-blooded)");
    }
    
    public void produceMilk() {
        System.out.println(this.name + " is producing milk for offspring");
    }
    
    public void growFur() {
        System.out.println(this.name + " is growing " + this.furColor + " fur");
    }
    
    // Override parent method
    @Override
    public void breathe() {
        System.out.println(this.name + " breathes air through lungs (mammalian breathing)");
    }
    
    // Override displayInfo to add mammal-specific information
    @Override
    public void displayInfo() {
        super.displayInfo(); // Call parent method first
        System.out.println("=== Mammal-Specific Information ===");
        System.out.println("Fur Color: " + this.furColor);
        System.out.println("Warm-Blooded: " + this.isWarmBlooded);
        System.out.println("Gestation Period: " + this.gestationPeriod + " days");
        System.out.println();
    }
    
    // Getters and setters for new properties
    public String getFurColor() { return this.furColor; }
    public void setFurColor(String furColor) { this.furColor = furColor; }
    
    public boolean isWarmBlooded() { return this.isWarmBlooded; }
    public int getGestationPeriod() { return this.gestationPeriod; }
    
    @Override
    public String toString() {
        return String.format("Mammal{name='%s', age=%d, species='%s', furColor='%s'}", 
                           name, age, species, furColor);
    }
}
```

Create `Dog.java` (Concrete Subclass):

```java
/**
 * Dog class extends Mammal - demonstrates concrete implementation
 */
public class Dog extends Mammal {
    
    // Dog-specific properties
    private String breed;
    private boolean isTrained;
    private String[] tricks;
    private int tricksLearned;
    
    // Default constructor
    public Dog() {
        super(); // Call Mammal constructor
        this.breed = "Mixed";
        this.isTrained = false;
        this.tricks = new String[10];
        this.tricksLearned = 0;
        this.species = "Canis lupus"; // Override species
        System.out.println("Dog default constructor called");
    }
    
    // Parameterized constructor
    public Dog(String name, int age, double weight, String furColor, String breed) {
        super(name, age, "Canis lupus", weight, furColor, 63); // 63 days gestation
        this.breed = breed;
        this.isTrained = false;
        this.tricks = new String[10];
        this.tricksLearned = 0;
        System.out.println("Dog parameterized constructor called for: " + name);
    }
    
    // Dog-specific methods
    public void bark() {
        System.out.println(this.name + " says: Woof! Woof!");
    }
    
    public void wagTail() {
        System.out.println(this.name + " is wagging tail happily");
    }
    
    public void fetch() {
        System.out.println(this.name + " is fetching the ball");
    }
    
    public void learnTrick(String trick) {
        if (this.tricksLearned < this.tricks.length) {
            this.tricks[this.tricksLearned] = trick;
            this.tricksLearned++;
            System.out.println(this.name + " learned a new trick: " + trick);
        } else {
            System.out.println(this.name + " has learned too many tricks already!");
        }
    }
    
    public void performTrick(String trick) {
        boolean canPerform = false;
        for (int i = 0; i < this.tricksLearned; i++) {
            if (this.tricks[i].equalsIgnoreCase(trick)) {
                canPerform = true;
                break;
            }
        }
        
        if (canPerform) {
            System.out.println(this.name + " performs trick: " + trick);
        } else {
            System.out.println(this.name + " doesn't know how to " + trick);
        }
    }
    
    // Override methods from parent classes
    @Override
    public void makeSound() {
        bark(); // Use dog-specific sound
    }
    
    @Override
    public void move() {
        System.out.println(this.name + " runs on four legs");
    }
    
    @Override
    public void eat() {
        System.out.println(this.name + " is eating dog food");
    }
    
    // Override displayInfo to show complete hierarchy information
    @Override
    public void displayInfo() {
        super.displayInfo(); // Call Mammal's displayInfo
        System.out.println("=== Dog-Specific Information ===");
        System.out.println("Breed: " + this.breed);
        System.out.println("Trained: " + this.isTrained);
        System.out.println("Tricks Learned: " + this.tricksLearned);
        if (this.tricksLearned > 0) {
            System.out.print("Known Tricks: ");
            for (int i = 0; i < this.tricksLearned; i++) {
                System.out.print(this.tricks[i]);
                if (i < this.tricksLearned - 1) System.out.print(", ");
            }
            System.out.println();
        }
        System.out.println();
    }
    
    // Getters and setters
    public String getBreed() { return this.breed; }
    public void setBreed(String breed) { this.breed = breed; }
    
    public boolean isTrained() { return this.isTrained; }
    public void setTrained(boolean trained) { 
        this.isTrained = trained;
        if (trained) {
            System.out.println(this.name + " is now trained!");
        }
    }
    
    public int getTricksLearned() { return this.tricksLearned; }
    
    @Override
    public String toString() {
        return String.format("Dog{name='%s', age=%d, breed='%s', tricks=%d}", 
                           name, age, breed, tricksLearned);
    }
    
    // Test the inheritance hierarchy
    public static void main(String[] args) {
        System.out.println("=== Inheritance Hierarchy Demo ===\n");
        
        // Test constructor chaining
        System.out.println("Creating a Dog object:");
        Dog myDog = new Dog("Buddy", 3, 25.5, "Golden", "Golden Retriever");
        System.out.println();
        
        // Test inherited methods
        System.out.println("=== Testing Inherited Methods ===");
        myDog.eat();           // Overridden in Dog
        myDog.sleep();         // Inherited from Animal
        myDog.breathe();       // Overridden in Mammal
        myDog.makeSound();     // Overridden in Dog
        myDog.move();          // Overridden in Dog
        myDog.age();           // Final method from Animal
        System.out.println();
        
        // Test mammal-specific methods
        System.out.println("=== Testing Mammal Methods ===");
        myDog.regulateBodyTemperature(); // Inherited from Mammal
        myDog.produceMilk();             // Inherited from Mammal
        myDog.growFur();                 // Inherited from Mammal
        System.out.println();
        
        // Test dog-specific methods
        System.out.println("=== Testing Dog-Specific Methods ===");
        myDog.bark();
        myDog.wagTail();
        myDog.fetch();
        myDog.learnTrick("sit");
        myDog.learnTrick("stay");
        myDog.learnTrick("roll over");
        myDog.performTrick("sit");
        myDog.performTrick("dance"); // Unknown trick
        myDog.setTrained(true);
        System.out.println();
        
        // Display complete information
        myDog.displayInfo();
        
        // Test polymorphism
        System.out.println("=== Testing Polymorphism ===");
        Animal animal = myDog; // Dog IS-A Animal
        animal.makeSound();    // Calls Dog's overridden method
        animal.move();         // Calls Dog's overridden method
        
        Mammal mammal = myDog; // Dog IS-A Mammal
        mammal.regulateBodyTemperature(); // Calls Mammal method
        
        System.out.println("Animal reference: " + animal);
        System.out.println("Mammal reference: " + mammal);
        System.out.println("Dog reference: " + myDog);
    }
}
```

---

## 🔄 Part 2: Method Overriding vs Overloading (20 minutes)

### **Understanding the Differences**

Create `OverrideOverloadDemo.java`:

```java
/**
 * Demonstrates the difference between method overriding and overloading
 */
class Parent {
    
    // Method to be overridden
    public void display() {
        System.out.println("Display method in Parent class");
    }
    
    // Method with different signatures for overloading
    public void print(String message) {
        System.out.println("Parent print: " + message);
    }
    
    public void calculate(int a, int b) {
        System.out.println("Parent calculate: " + (a + b));
    }
    
    // Static method - can be hidden but not overridden
    public static void staticMethod() {
        System.out.println("Static method in Parent");
    }
    
    // Final method - cannot be overridden
    public final void finalMethod() {
        System.out.println("Final method in Parent - cannot be overridden");
    }
    
    // Private method - not inherited
    private void privateMethod() {
        System.out.println("Private method in Parent");
    }
}

class Child extends Parent {
    
    // METHOD OVERRIDING - same signature as parent
    @Override
    public void display() {
        System.out.println("Display method OVERRIDDEN in Child class");
        super.display(); // Call parent version
    }
    
    // METHOD OVERLOADING - different signatures
    public void print(String message, int count) {
        for (int i = 0; i < count; i++) {
            System.out.println("Child print #" + (i + 1) + ": " + message);
        }
    }
    
    public void print(int number) {
        System.out.println("Child print number: " + number);
    }
    
    // OVERRIDING with different return type (covariant return)
    @Override
    public void calculate(int a, int b) {
        System.out.println("Child calculate (overridden): " + (a * b));
    }
    
    // METHOD HIDING - static method with same signature
    public static void staticMethod() {
        System.out.println("Static method in Child (HIDING parent)");
    }
    
    // Cannot override final method - this would cause compilation error:
    // public void finalMethod() { ... } // ERROR!
    
    // Can create private method with same name (not overriding)
    private void privateMethod() {
        System.out.println("Private method in Child (not overriding)");
    }
    
    // Additional overloaded methods
    public void display(String prefix) {
        System.out.println(prefix + ": Display with parameter in Child");
    }
    
    public void display(String prefix, int count) {
        for (int i = 0; i < count; i++) {
            System.out.println(prefix + " #" + (i + 1) + ": Display in Child");
        }
    }
}

public class OverrideOverloadDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Method Overriding vs Overloading Demo ===\n");
        
        // Create objects
        Parent parent = new Parent();
        Child child = new Child();
        Parent polymorphicChild = new Child(); // Polymorphism
        
        System.out.println("=== Method Overriding Demo ===");
        parent.display();           // Calls Parent version
        child.display();            // Calls Child version (overridden)
        polymorphicChild.display(); // Calls Child version (runtime polymorphism)
        System.out.println();
        
        System.out.println("=== Method Overloading Demo ===");
        // Parent class overloaded methods
        parent.print("Hello");
        parent.calculate(5, 3);
        
        // Child class overloaded methods
        child.print("Hello");              // Inherited from Parent
        child.print("Hello", 2);           // Overloaded in Child
        child.print(42);                   // Overloaded in Child
        child.calculate(5, 3);             // Overridden in Child
        
        // Child display overloading
        child.display();                   // Overridden version
        child.display("Prefix");           // Overloaded version
        child.display("Count", 3);         // Overloaded version
        System.out.println();
        
        System.out.println("=== Static Method Hiding ===");
        Parent.staticMethod();     // Parent static method
        Child.staticMethod();      // Child static method (hiding)
        
        // With polymorphic reference
        polymorphicChild.staticMethod(); // Still calls Parent (compile-time binding)
        System.out.println();
        
        System.out.println("=== Final Method Demo ===");
        parent.finalMethod();      // Can call final method
        child.finalMethod();       // Inherited final method
        polymorphicChild.finalMethod(); // Final method through polymorphism
        System.out.println();
        
        // Demonstrate method signature importance
        System.out.println("=== Method Signatures ===");
        demonstrateMethodSignatures();
    }
    
    public static void demonstrateMethodSignatures() {
        Child child = new Child();
        
        // These are all different methods due to different signatures
        child.display();              // display()
        child.display("Test");        // display(String)
        child.display("Test", 2);     // display(String, int)
        
        child.print("Message");       // print(String) - inherited
        child.print("Message", 3);    // print(String, int) - child's
        child.print(100);             // print(int) - child's
        
        System.out.println("\nMethod signature components:");
        System.out.println("1. Method name");
        System.out.println("2. Parameter list (type, order, count)");
        System.out.println("3. Return type is NOT part of signature for overloading");
        System.out.println("4. Access modifier is NOT part of signature");
    }
}
```

---

## 🏗️ Part 3: Constructor Chaining and Super Keyword (20 minutes)

### **Understanding Constructor Flow**

Create `ConstructorChaining.java`:

```java
/**
 * Demonstrates constructor chaining and super keyword usage
 */
class Vehicle {
    protected String brand;
    protected String model;
    protected int year;
    protected double price;
    
    // Default constructor
    public Vehicle() {
        this.brand = "Unknown";
        this.model = "Unknown";
        this.year = 2020;
        this.price = 0.0;
        System.out.println("Vehicle default constructor called");
    }
    
    // Parameterized constructor
    public Vehicle(String brand, String model, int year, double price) {
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.price = price;
        System.out.println("Vehicle parameterized constructor called: " + brand + " " + model);
    }
    
    // Copy constructor
    public Vehicle(Vehicle other) {
        this.brand = other.brand;
        this.model = other.model;
        this.year = other.year;
        this.price = other.price;
        System.out.println("Vehicle copy constructor called");
    }
    
    public void startEngine() {
        System.out.println("Starting " + this.brand + " " + this.model + " engine");
    }
    
    public void displayInfo() {
        System.out.println("Vehicle: " + this.year + " " + this.brand + " " + this.model + 
                          " - $" + String.format("%.2f", this.price));
    }
    
    // Getters
    public String getBrand() { return this.brand; }
    public String getModel() { return this.model; }
    public int getYear() { return this.year; }
    public double getPrice() { return this.price; }
}

class Car extends Vehicle {
    private int numberOfDoors;
    private String fuelType;
    private boolean isAutomatic;
    
    // Default constructor - calls parent default constructor
    public Car() {
        super(); // Must be first statement if present
        this.numberOfDoors = 4;
        this.fuelType = "Gasoline";
        this.isAutomatic = true;
        System.out.println("Car default constructor called");
    }
    
    // Constructor calling parent parameterized constructor
    public Car(String brand, String model, int year, double price, 
               int numberOfDoors, String fuelType, boolean isAutomatic) {
        super(brand, model, year, price); // Call parent constructor
        this.numberOfDoors = numberOfDoors;
        this.fuelType = fuelType;
        this.isAutomatic = isAutomatic;
        System.out.println("Car parameterized constructor called");
    }
    
    // Constructor with some default values
    public Car(String brand, String model, int year, double price) {
        this(brand, model, year, price, 4, "Gasoline", true); // Call other constructor
        System.out.println("Car constructor with defaults called");
    }
    
    // Copy constructor
    public Car(Car other) {
        super(other); // Call parent copy constructor
        this.numberOfDoors = other.numberOfDoors;
        this.fuelType = other.fuelType;
        this.isAutomatic = other.isAutomatic;
        System.out.println("Car copy constructor called");
    }
    
    @Override
    public void startEngine() {
        System.out.print("Car: ");
        super.startEngine(); // Call parent method
        System.out.println("Car-specific startup sequence initiated");
    }
    
    @Override
    public void displayInfo() {
        super.displayInfo(); // Call parent method first
        System.out.println("Car Details: " + this.numberOfDoors + " doors, " + 
                          this.fuelType + " fuel, " + 
                          (this.isAutomatic ? "Automatic" : "Manual") + " transmission");
    }
    
    public void openTrunk() {
        System.out.println("Opening trunk of " + super.getBrand() + " " + super.getModel());
    }
    
    // Getters
    public int getNumberOfDoors() { return this.numberOfDoors; }
    public String getFuelType() { return this.fuelType; }
    public boolean isAutomatic() { return this.isAutomatic; }
}

class ElectricCar extends Car {
    private double batteryCapacity; // in kWh
    private int range; // in miles
    private boolean fastChargingSupport;
    
    // Default constructor
    public ElectricCar() {
        super(); // Call Car default constructor
        this.batteryCapacity = 50.0;
        this.range = 200;
        this.fastChargingSupport = true;
        // Override fuel type for electric car
        this.fuelType = "Electric";
        System.out.println("ElectricCar default constructor called");
    }
    
    // Parameterized constructor
    public ElectricCar(String brand, String model, int year, double price,
                      int numberOfDoors, double batteryCapacity, int range) {
        super(brand, model, year, price, numberOfDoors, "Electric", true);
        this.batteryCapacity = batteryCapacity;
        this.range = range;
        this.fastChargingSupport = true;
        System.out.println("ElectricCar parameterized constructor called");
    }
    
    // Constructor with minimal parameters
    public ElectricCar(String brand, String model, int year, double price) {
        this(brand, model, year, price, 4, 75.0, 300);
        System.out.println("ElectricCar constructor with defaults called");
    }
    
    @Override
    public void startEngine() {
        System.out.println("Electric Car: Silent startup - no engine sound!");
        System.out.println("Battery level checked, systems initialized");
        // Note: not calling super.startEngine() because electric cars work differently
    }
    
    @Override
    public void displayInfo() {
        super.displayInfo(); // Call Car's displayInfo
        System.out.println("Electric Car Details: " + this.batteryCapacity + " kWh battery, " + 
                          this.range + " miles range, Fast charging: " + 
                          (this.fastChargingSupport ? "Yes" : "No"));
    }
    
    public void chargeBattery() {
        System.out.println("Charging " + super.getBrand() + " " + super.getModel() + 
                          " battery (" + this.batteryCapacity + " kWh)");
    }
    
    public void checkRange() {
        System.out.println("Current range: " + this.range + " miles on full charge");
    }
    
    // Getters
    public double getBatteryCapacity() { return this.batteryCapacity; }
    public int getRange() { return this.range; }
    public boolean hasFastChargingSupport() { return this.fastChargingSupport; }
}

public class ConstructorChaining {
    
    public static void main(String[] args) {
        System.out.println("=== Constructor Chaining Demo ===\n");
        
        // Test constructor chaining
        System.out.println("1. Creating Vehicle:");
        Vehicle vehicle = new Vehicle("Toyota", "Generic", 2023, 25000);
        System.out.println();
        
        System.out.println("2. Creating Car with default constructor:");
        Car car1 = new Car();
        System.out.println();
        
        System.out.println("3. Creating Car with full parameters:");
        Car car2 = new Car("Honda", "Civic", 2023, 28000, 4, "Gasoline", false);
        System.out.println();
        
        System.out.println("4. Creating Car with some defaults:");
        Car car3 = new Car("Ford", "Focus", 2023, 24000);
        System.out.println();
        
        System.out.println("5. Creating ElectricCar with default constructor:");
        ElectricCar eCar1 = new ElectricCar();
        System.out.println();
        
        System.out.println("6. Creating ElectricCar with parameters:");
        ElectricCar eCar2 = new ElectricCar("Tesla", "Model 3", 2023, 45000, 4, 75.0, 350);
        System.out.println();
        
        System.out.println("7. Creating ElectricCar with minimal parameters:");
        ElectricCar eCar3 = new ElectricCar("Nissan", "Leaf", 2023, 32000);
        System.out.println();
        
        // Test method overriding with super
        System.out.println("=== Testing Method Overriding with Super ===");
        System.out.println("Vehicle startEngine():");
        vehicle.startEngine();
        System.out.println();
        
        System.out.println("Car startEngine():");
        car2.startEngine();
        System.out.println();
        
        System.out.println("ElectricCar startEngine():");
        eCar2.startEngine();
        System.out.println();
        
        // Test displayInfo hierarchy
        System.out.println("=== Testing displayInfo Hierarchy ===");
        System.out.println("Car displayInfo():");
        car2.displayInfo();
        System.out.println();
        
        System.out.println("ElectricCar displayInfo():");
        eCar2.displayInfo();
        System.out.println();
        
        // Test copy constructors
        System.out.println("=== Testing Copy Constructors ===");
        Car carCopy = new Car(car2);
        ElectricCar eCarCopy = new ElectricCar(eCar2);
        System.out.println();
        
        // Test accessing parent methods with super
        System.out.println("=== Testing Super Keyword Usage ===");
        car2.openTrunk(); // Method that uses super to access parent properties
        eCar2.chargeBattery(); // Method that uses super to access parent properties
        eCar2.checkRange();
    }
}
```

---

## 🎯 Today's Major Challenge

### **Challenge: Employee Hierarchy System**

Create `EmployeeHierarchy.java`:

```java
import java.time.LocalDate;
import java.time.Period;

/**
 * Complete Employee Hierarchy demonstrating inheritance concepts
 */

// Base Employee class
abstract class Employee {
    protected String employeeId;
    protected String firstName;
    protected String lastName;
    protected String email;
    protected LocalDate hireDate;
    protected double baseSalary;
    protected String department;
    protected boolean isActive;
    
    private static int nextEmployeeId = 1001;
    
    // Constructor
    public Employee(String firstName, String lastName, String email, 
                   double baseSalary, String department) {
        this.employeeId = "EMP" + String.format("%04d", nextEmployeeId++);
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.baseSalary = baseSalary;
        this.department = department;
        this.hireDate = LocalDate.now();
        this.isActive = true;
        System.out.println("Employee constructor called for: " + getFullName());
    }
    
    // Abstract methods - must be implemented by subclasses
    public abstract double calculateSalary();
    public abstract String getJobTitle();
    public abstract void performDuties();
    
    // Concrete methods available to all employees
    public String getFullName() {
        return this.firstName + " " + this.lastName;
    }
    
    public int getYearsOfService() {
        return Period.between(this.hireDate, LocalDate.now()).getYears();
    }
    
    public void promote(double salaryIncrease) {
        this.baseSalary += salaryIncrease;
        System.out.println(getFullName() + " promoted with $" + 
                          String.format("%.2f", salaryIncrease) + " salary increase");
    }
    
    public void displayInfo() {
        System.out.println("=== Employee Information ===");
        System.out.println("ID: " + this.employeeId);
        System.out.println("Name: " + getFullName());
        System.out.println("Email: " + this.email);
        System.out.println("Title: " + getJobTitle());
        System.out.println("Department: " + this.department);
        System.out.println("Base Salary: $" + String.format("%.2f", this.baseSalary));
        System.out.println("Total Compensation: $" + String.format("%.2f", calculateSalary()));
        System.out.println("Hire Date: " + this.hireDate);
        System.out.println("Years of Service: " + getYearsOfService());
        System.out.println("Status: " + (this.isActive ? "Active" : "Inactive"));
    }
    
    // Getters
    public String getEmployeeId() { return this.employeeId; }
    public String getFirstName() { return this.firstName; }
    public String getLastName() { return this.lastName; }
    public String getEmail() { return this.email; }
    public LocalDate getHireDate() { return this.hireDate; }
    public double getBaseSalary() { return this.baseSalary; }
    public String getDepartment() { return this.department; }
    public boolean isActive() { return this.isActive; }
    
    @Override
    public String toString() {
        return String.format("%s{id='%s', name='%s', title='%s', salary=%.2f}", 
                           getClass().getSimpleName(), employeeId, getFullName(), 
                           getJobTitle(), calculateSalary());
    }
}

// Full-time employee class
class FullTimeEmployee extends Employee {
    private double benefits;
    private int vacationDays;
    private boolean hasHealthInsurance;
    
    public FullTimeEmployee(String firstName, String lastName, String email,
                           double baseSalary, String department, double benefits, 
                           int vacationDays) {
        super(firstName, lastName, email, baseSalary, department);
        this.benefits = benefits;
        this.vacationDays = vacationDays;
        this.hasHealthInsurance = true;
        System.out.println("FullTimeEmployee constructor called");
    }
    
    @Override
    public double calculateSalary() {
        return this.baseSalary + this.benefits;
    }
    
    @Override
    public String getJobTitle() {
        return "Full-Time Employee";
    }
    
    @Override
    public void performDuties() {
        System.out.println(getFullName() + " is performing full-time duties (40 hours/week)");
    }
    
    public void takeVacation(int days) {
        if (days <= this.vacationDays) {
            this.vacationDays -= days;
            System.out.println(getFullName() + " took " + days + " vacation days. " +
                             "Remaining: " + this.vacationDays);
        } else {
            System.out.println("Insufficient vacation days. Available: " + this.vacationDays);
        }
    }
    
    @Override
    public void displayInfo() {
        super.displayInfo();
        System.out.println("=== Full-Time Specific Info ===");
        System.out.println("Benefits: $" + String.format("%.2f", this.benefits));
        System.out.println("Vacation Days: " + this.vacationDays);
        System.out.println("Health Insurance: " + (this.hasHealthInsurance ? "Yes" : "No"));
        System.out.println();
    }
    
    public double getBenefits() { return this.benefits; }
    public int getVacationDays() { return this.vacationDays; }
    public boolean hasHealthInsurance() { return this.hasHealthInsurance; }
}

// Part-time employee class
class PartTimeEmployee extends Employee {
    private int hoursPerWeek;
    private double hourlyRate;
    
    public PartTimeEmployee(String firstName, String lastName, String email,
                           double hourlyRate, int hoursPerWeek, String department) {
        super(firstName, lastName, email, 0, department); // No base salary
        this.hourlyRate = hourlyRate;
        this.hoursPerWeek = hoursPerWeek;
        System.out.println("PartTimeEmployee constructor called");
    }
    
    @Override
    public double calculateSalary() {
        return this.hourlyRate * this.hoursPerWeek * 52; // Annual salary
    }
    
    @Override
    public String getJobTitle() {
        return "Part-Time Employee";
    }
    
    @Override
    public void performDuties() {
        System.out.println(getFullName() + " is performing part-time duties (" + 
                          this.hoursPerWeek + " hours/week)");
    }
    
    public void adjustHours(int newHours) {
        int oldHours = this.hoursPerWeek;
        this.hoursPerWeek = Math.max(0, Math.min(39, newHours)); // Max 39 hours
        System.out.println(getFullName() + " hours adjusted from " + oldHours + 
                          " to " + this.hoursPerWeek + " hours/week");
    }
    
    @Override
    public void displayInfo() {
        super.displayInfo();
        System.out.println("=== Part-Time Specific Info ===");
        System.out.println("Hourly Rate: $" + String.format("%.2f", this.hourlyRate));
        System.out.println("Hours per Week: " + this.hoursPerWeek);
        System.out.println("Annual Hours: " + (this.hoursPerWeek * 52));
        System.out.println();
    }
    
    public int getHoursPerWeek() { return this.hoursPerWeek; }
    public double getHourlyRate() { return this.hourlyRate; }
}

// Manager class extending FullTimeEmployee
class Manager extends FullTimeEmployee {
    private int teamSize;
    private double managementBonus;
    private String[] directReports;
    
    public Manager(String firstName, String lastName, String email,
                  double baseSalary, String department, double benefits,
                  int vacationDays, int teamSize, double managementBonus) {
        super(firstName, lastName, email, baseSalary, department, benefits, vacationDays);
        this.teamSize = teamSize;
        this.managementBonus = managementBonus;
        this.directReports = new String[teamSize];
        System.out.println("Manager constructor called");
    }
    
    @Override
    public double calculateSalary() {
        return super.calculateSalary() + this.managementBonus;
    }
    
    @Override
    public String getJobTitle() {
        return "Manager";
    }
    
    @Override
    public void performDuties() {
        super.performDuties();
        System.out.println(getFullName() + " is managing a team of " + this.teamSize + " people");
    }
    
    public void addDirectReport(String employeeName) {
        for (int i = 0; i < this.directReports.length; i++) {
            if (this.directReports[i] == null) {
                this.directReports[i] = employeeName;
                System.out.println(employeeName + " added as direct report to " + getFullName());
                return;
            }
        }
        System.out.println("Cannot add more direct reports. Team is full.");
    }
    
    public void conductTeamMeeting() {
        System.out.println(getFullName() + " is conducting a team meeting");
        System.out.println("Team members present:");
        for (String report : this.directReports) {
            if (report != null) {
                System.out.println("  - " + report);
            }
        }
    }
    
    @Override
    public void displayInfo() {
        super.displayInfo();
        System.out.println("=== Manager Specific Info ===");
        System.out.println("Team Size: " + this.teamSize);
        System.out.println("Management Bonus: $" + String.format("%.2f", this.managementBonus));
        System.out.print("Direct Reports: ");
        boolean hasReports = false;
        for (String report : this.directReports) {
            if (report != null) {
                if (hasReports) System.out.print(", ");
                System.out.print(report);
                hasReports = true;
            }
        }
        if (!hasReports) System.out.print("None");
        System.out.println();
        System.out.println();
    }
    
    public int getTeamSize() { return this.teamSize; }
    public double getManagementBonus() { return this.managementBonus; }
}

// Contract employee class
final class ContractEmployee extends Employee {
    private LocalDate contractStartDate;
    private LocalDate contractEndDate;
    private double contractAmount;
    private boolean isRenewable;
    
    public ContractEmployee(String firstName, String lastName, String email,
                           double contractAmount, String department,
                           LocalDate contractEndDate, boolean isRenewable) {
        super(firstName, lastName, email, 0, department);
        this.contractStartDate = LocalDate.now();
        this.contractEndDate = contractEndDate;
        this.contractAmount = contractAmount;
        this.isRenewable = isRenewable;
        System.out.println("ContractEmployee constructor called");
    }
    
    @Override
    public final double calculateSalary() {
        // Convert contract amount to annual equivalent
        long contractDays = Period.between(contractStartDate, contractEndDate).getDays();
        return (contractAmount / contractDays) * 365;
    }
    
    @Override
    public final String getJobTitle() {
        return "Contract Employee";
    }
    
    @Override
    public final void performDuties() {
        System.out.println(getFullName() + " is performing contract duties until " + 
                          this.contractEndDate);
    }
    
    public boolean isContractActive() {
        return LocalDate.now().isBefore(this.contractEndDate);
    }
    
    public void renewContract(LocalDate newEndDate, double newAmount) {
        if (this.isRenewable) {
            this.contractEndDate = newEndDate;
            this.contractAmount = newAmount;
            System.out.println("Contract renewed for " + getFullName() + 
                             " until " + newEndDate);
        } else {
            System.out.println("Contract for " + getFullName() + " is not renewable");
        }
    }
    
    @Override
    public final void displayInfo() {
        super.displayInfo();
        System.out.println("=== Contract Specific Info ===");
        System.out.println("Contract Start: " + this.contractStartDate);
        System.out.println("Contract End: " + this.contractEndDate);
        System.out.println("Contract Amount: $" + String.format("%.2f", this.contractAmount));
        System.out.println("Renewable: " + (this.isRenewable ? "Yes" : "No"));
        System.out.println("Contract Active: " + (isContractActive() ? "Yes" : "No"));
        System.out.println();
    }
    
    public LocalDate getContractStartDate() { return this.contractStartDate; }
    public LocalDate getContractEndDate() { return this.contractEndDate; }
    public double getContractAmount() { return this.contractAmount; }
    public boolean isRenewable() { return this.isRenewable; }
}

public class EmployeeHierarchy {
    
    public static void main(String[] args) {
        System.out.println("=== Employee Hierarchy System Demo ===\n");
        
        // Create different types of employees
        System.out.println("=== Creating Employees ===");
        
        FullTimeEmployee fullTime = new FullTimeEmployee(
            "John", "Smith", "john.smith@company.com",
            75000, "Engineering", 15000, 25
        );
        
        PartTimeEmployee partTime = new PartTimeEmployee(
            "Jane", "Doe", "jane.doe@company.com",
            25.0, 20, "Marketing"
        );
        
        Manager manager = new Manager(
            "Bob", "Johnson", "bob.johnson@company.com",
            95000, "Engineering", 20000, 30, 5, 15000
        );
        
        ContractEmployee contractor = new ContractEmployee(
            "Alice", "Brown", "alice.brown@company.com",
            80000, "IT", LocalDate.now().plusMonths(6), true
        );
        
        System.out.println();
        
        // Test polymorphism
        System.out.println("=== Testing Polymorphism ===");
        Employee[] employees = {fullTime, partTime, manager, contractor};
        
        for (Employee emp : employees) {
            System.out.println("Employee: " + emp.getFullName());
            System.out.println("Title: " + emp.getJobTitle());
            System.out.println("Salary: $" + String.format("%.2f", emp.calculateSalary()));
            emp.performDuties();
            System.out.println();
        }
        
        // Test specific functionality
        System.out.println("=== Testing Specific Functionality ===");
        
        // Full-time employee operations
        fullTime.takeVacation(5);
        fullTime.promote(5000);
        
        // Part-time employee operations
        partTime.adjustHours(25);
        
        // Manager operations
        manager.addDirectReport("John Smith");
        manager.addDirectReport("Sarah Wilson");
        manager.addDirectReport("Mike Davis");
        manager.conductTeamMeeting();
        
        // Contract employee operations
        contractor.renewContract(LocalDate.now().plusYears(1), 90000);
        
        System.out.println();
        
        // Display detailed information
        System.out.println("=== Detailed Employee Information ===");
        fullTime.displayInfo();
        partTime.displayInfo();
        manager.displayInfo();
        contractor.displayInfo();
        
        // Test inheritance hierarchy
        System.out.println("=== Testing Inheritance Hierarchy ===");
        System.out.println("Manager IS-A FullTimeEmployee: " + (manager instanceof FullTimeEmployee));
        System.out.println("Manager IS-A Employee: " + (manager instanceof Employee));
        System.out.println("PartTime IS-A Employee: " + (partTime instanceof Employee));
        System.out.println("Contract IS-A FullTimeEmployee: " + (contractor instanceof FullTimeEmployee));
        
        // Test method overriding
        System.out.println("\n=== Method Overriding Test ===");
        Employee emp = manager; // Polymorphic reference
        emp.performDuties(); // Calls Manager's overridden method
        System.out.println("Polymorphic call result: " + emp.calculateSalary());
    }
}
```

---

## 📝 Key Takeaways from Day 7

### **Inheritance Fundamentals:**
- ✅ **IS-A relationship:** When to use inheritance vs composition
- ✅ **extends keyword:** Creating parent-child relationships
- ✅ **Constructor chaining:** How constructors flow through hierarchy
- ✅ **Access modifiers:** protected for inheritance, private for encapsulation

### **Method Concepts:**
- ✅ **Method overriding:** Same signature, different implementation
- ✅ **Method overloading:** Same name, different signatures
- ✅ **super keyword:** Accessing parent class methods and constructors
- ✅ **final methods:** Preventing method overriding

### **Object Class Integration:**
- ✅ **toString():** String representation of objects
- ✅ **equals():** Object equality comparison
- ✅ **hashCode():** Hash table support
- ✅ **Polymorphism:** One interface, multiple implementations

### **Advanced Concepts:**
- ✅ **Abstract classes:** Partial implementations requiring completion
- ✅ **final classes:** Preventing inheritance (like String)
- ✅ **Constructor patterns:** Chaining and delegation
- ✅ **Method hiding:** Static method behavior in inheritance

---

## 🚀 Tomorrow's Preview (Day 8 - July 17)

**Focus:** Polymorphism & Abstraction
- Abstract classes and methods in detail
- Interface definition and implementation
- Multiple inheritance through interfaces
- Runtime polymorphism and dynamic method dispatch
- Designing flexible object hierarchies

---

## ✅ Day 7 Checklist

**Completed Tasks:**
- [ ] Create inheritance hierarchies with extends keyword
- [ ] Master method overriding vs overloading differences
- [ ] Use super keyword for parent class access
- [ ] Override Object class methods (toString, equals, hashCode)
- [ ] Implement constructor chaining properly
- [ ] Apply final keyword to prevent inheritance/overriding
- [ ] Build complete Employee Hierarchy System
- [ ] Commit code to GitHub: "Day 7: Inheritance and Class Hierarchies mastery"

**Bonus Achievements:**
- [ ] Create additional animal hierarchy classes
- [ ] Implement more Object methods (clone, finalize)
- [ ] Design a vehicle hierarchy with multiple levels
- [ ] Experiment with abstract classes and methods

---

## 🎉 Object-Oriented Mastery Unlocked!

Congratulations! You've now mastered **inheritance** - one of the pillars of Object-Oriented Programming! You can now:

- **Build rich class hierarchies** that model real-world relationships
- **Maximize code reuse** through proper inheritance design
- **Override methods** to provide specialized behavior
- **Use polymorphism** to write flexible, maintainable code

Tomorrow we'll explore **polymorphism and abstraction** - the advanced OOP concepts that make Java applications truly powerful and flexible!

**You're thinking like a professional object-oriented developer now!** 🚀