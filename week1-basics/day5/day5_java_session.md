# Day 5: Classes and Objects - OOP Foundation
**Date:** July 14, 2025  
**Duration:** 1.5 hours (Combined Session)  
**Focus:** Class definition, object creation, constructors, and the `this` keyword

---

## 🎯 Today's Learning Goals
By the end of this session, you'll:
- ✅ Create custom classes with instance variables and methods
- ✅ Understand object creation and memory allocation
- ✅ Master constructors (default, parameterized, overloaded)
- ✅ Use the `this` keyword effectively
- ✅ Implement access modifiers for data protection
- ✅ Design real-world object models
- ✅ Build a complete object-oriented application

---

## 📚 Part 1: Class Fundamentals (25 minutes)

### **What is a Class?**
A class is a blueprint or template for creating objects. It defines:
- **State** (instance variables/fields)
- **Behavior** (methods)
- **Constructor** (how objects are created)

### **Basic Class Structure**

Create `Student.java`:

```java
public class Student {
    // Instance variables (state) - each object has its own copy
    private String name;
    private int age;
    private String studentId;
    private double gpa;
    private String major;
    
    // Default constructor (no parameters)
    public Student() {
        this.name = "Unknown";
        this.age = 18;
        this.studentId = "STU000";
        this.gpa = 0.0;
        this.major = "Undeclared";
        System.out.println("Student object created with default values");
    }
    
    // Parameterized constructor
    public Student(String name, int age, String studentId, String major) {
        this.name = name;
        this.age = age;
        this.studentId = studentId;
        this.major = major;
        this.gpa = 0.0; // New students start with 0.0 GPA
        System.out.println("Student " + name + " created successfully");
    }
    
    // Overloaded constructor with GPA
    public Student(String name, int age, String studentId, String major, double gpa) {
        this.name = name;
        this.age = age;
        this.studentId = studentId;
        this.major = major;
        this.gpa = gpa;
        System.out.println("Student " + name + " created with GPA " + gpa);
    }
    
    // Instance methods (behavior)
    public void displayInfo() {
        System.out.println("=== Student Information ===");
        System.out.println("Name: " + this.name);
        System.out.println("Age: " + this.age);
        System.out.println("Student ID: " + this.studentId);
        System.out.println("Major: " + this.major);
        System.out.printf("GPA: %.2f%n", this.gpa);
        System.out.println("Academic Status: " + this.getAcademicStatus());
        System.out.println();
    }
    
    public void updateGPA(double newGPA) {
        if (newGPA >= 0.0 && newGPA <= 4.0) {
            double oldGPA = this.gpa;
            this.gpa = newGPA;
            System.out.printf("%s's GPA updated from %.2f to %.2f%n", this.name, oldGPA, newGPA);
        } else {
            System.out.println("Invalid GPA! Must be between 0.0 and 4.0");
        }
    }
    
    public void changeMajor(String newMajor) {
        String oldMajor = this.major;
        this.major = newMajor;
        System.out.println(this.name + " changed major from " + oldMajor + " to " + newMajor);
    }
    
    public String getAcademicStatus() {
        if (this.gpa >= 3.5) {
            return "Dean's List";
        } else if (this.gpa >= 3.0) {
            return "Good Standing";
        } else if (this.gpa >= 2.0) {
            return "Academic Warning";
        } else {
            return "Academic Probation";
        }
    }
    
    public boolean isEligibleForGraduation(int requiredCredits, int earnedCredits) {
        return this.gpa >= 2.0 && earnedCredits >= requiredCredits;
    }
    
    // Getter methods (accessors)
    public String getName() {
        return this.name;
    }
    
    public int getAge() {
        return this.age;
    }
    
    public String getStudentId() {
        return this.studentId;
    }
    
    public double getGPA() {
        return this.gpa;
    }
    
    public String getMajor() {
        return this.major;
    }
    
    // Setter methods (mutators)
    public void setName(String name) {
        if (name != null && !name.trim().isEmpty()) {
            this.name = name;
        } else {
            System.out.println("Invalid name provided");
        }
    }
    
    public void setAge(int age) {
        if (age >= 16 && age <= 65) {
            this.age = age;
        } else {
            System.out.println("Invalid age! Must be between 16 and 65");
        }
    }
    
    // toString method for easy printing
    @Override
    public String toString() {
        return String.format("Student{name='%s', age=%d, id='%s', major='%s', gpa=%.2f}", 
                           name, age, studentId, major, gpa);
    }
    
    // Test the Student class
    public static void main(String[] args) {
        System.out.println("=== Student Class Demo ===\n");
        
        // Create students using different constructors
        Student student1 = new Student(); // Default constructor
        student1.displayInfo();
        
        Student student2 = new Student("Alice Johnson", 20, "STU001", "Computer Science");
        student2.displayInfo();
        
        Student student3 = new Student("Bob Smith", 22, "STU002", "Mathematics", 3.7);
        student3.displayInfo();
        
        // Test methods
        System.out.println("=== Testing Student Methods ===");
        student2.updateGPA(3.8);
        student2.changeMajor("Software Engineering");
        student2.displayInfo();
        
        // Test validation
        student2.updateGPA(5.0); // Invalid GPA
        student2.setAge(10);     // Invalid age
        
        // Test graduation eligibility
        boolean canGraduate = student3.isEligibleForGraduation(120, 125);
        System.out.println(student3.getName() + " can graduate: " + canGraduate);
        
        // Test toString method
        System.out.println("\n=== Using toString ===");
        System.out.println(student1);
        System.out.println(student2);
        System.out.println(student3);
    }
}
```

---

## 🏦 Part 2: Real-World Class Example (20 minutes)

### **Bank Account Class**

Create `BankAccount.java`:

```java
public class BankAccount {
    // Private instance variables (encapsulation)
    private String accountNumber;
    private String accountHolderName;
    private double balance;
    private String accountType;
    private boolean isActive;
    
    // Static variable to generate unique account numbers
    private static int nextAccountNumber = 1001;
    
    // Default constructor
    public BankAccount() {
        this.accountNumber = "ACC" + nextAccountNumber++;
        this.accountHolderName = "Unknown";
        this.balance = 0.0;
        this.accountType = "Savings";
        this.isActive = true;
        System.out.println("Bank account created: " + this.accountNumber);
    }
    
    // Parameterized constructor
    public BankAccount(String accountHolderName, double initialDeposit, String accountType) {
        this.accountNumber = "ACC" + nextAccountNumber++;
        this.accountHolderName = accountHolderName;
        this.balance = initialDeposit >= 0 ? initialDeposit : 0.0;
        this.accountType = accountType;
        this.isActive = true;
        
        System.out.printf("Bank account created for %s with initial deposit $%.2f%n", 
                         accountHolderName, this.balance);
    }
    
    // Deposit money
    public void deposit(double amount) {
        if (!this.isActive) {
            System.out.println("Cannot deposit to inactive account");
            return;
        }
        
        if (amount > 0) {
            this.balance += amount;
            System.out.printf("Deposited $%.2f. New balance: $%.2f%n", amount, this.balance);
        } else {
            System.out.println("Deposit amount must be positive");
        }
    }
    
    // Withdraw money
    public boolean withdraw(double amount) {
        if (!this.isActive) {
            System.out.println("Cannot withdraw from inactive account");
            return false;
        }
        
        if (amount <= 0) {
            System.out.println("Withdrawal amount must be positive");
            return false;
        }
        
        if (amount > this.balance) {
            System.out.printf("Insufficient funds. Available balance: $%.2f%n", this.balance);
            return false;
        }
        
        this.balance -= amount;
        System.out.printf("Withdrew $%.2f. New balance: $%.2f%n", amount, this.balance);
        return true;
    }
    
    // Transfer money to another account
    public boolean transfer(BankAccount targetAccount, double amount) {
        if (this.withdraw(amount)) {
            targetAccount.deposit(amount);
            System.out.printf("Transferred $%.2f from %s to %s%n", 
                             amount, this.accountNumber, targetAccount.accountNumber);
            return true;
        }
        return false;
    }
    
    // Calculate interest (simple interest)
    public void addInterest(double interestRate) {
        if (this.isActive && this.balance > 0) {
            double interest = this.balance * (interestRate / 100);
            this.balance += interest;
            System.out.printf("Interest added: $%.2f at %.2f%% rate. New balance: $%.2f%n", 
                             interest, interestRate, this.balance);
        }
    }
    
    // Close account
    public void closeAccount() {
        if (this.balance > 0) {
            System.out.printf("Cannot close account with remaining balance: $%.2f%n", this.balance);
        } else {
            this.isActive = false;
            System.out.println("Account " + this.accountNumber + " has been closed");
        }
    }
    
    // Account statement
    public void printStatement() {
        System.out.println("=== BANK STATEMENT ===");
        System.out.println("Account Number: " + this.accountNumber);
        System.out.println("Account Holder: " + this.accountHolderName);
        System.out.println("Account Type: " + this.accountType);
        System.out.printf("Current Balance: $%.2f%n", this.balance);
        System.out.println("Account Status: " + (this.isActive ? "Active" : "Closed"));
        System.out.println("=====================");
        System.out.println();
    }
    
    // Getter methods
    public String getAccountNumber() {
        return this.accountNumber;
    }
    
    public String getAccountHolderName() {
        return this.accountHolderName;
    }
    
    public double getBalance() {
        return this.balance;
    }
    
    public String getAccountType() {
        return this.accountType;
    }
    
    public boolean isActive() {
        return this.isActive;
    }
    
    // Setter methods (with validation)
    public void setAccountHolderName(String name) {
        if (name != null && !name.trim().isEmpty()) {
            this.accountHolderName = name;
            System.out.println("Account holder name updated to: " + name);
        } else {
            System.out.println("Invalid name provided");
        }
    }
    
    @Override
    public String toString() {
        return String.format("BankAccount{number='%s', holder='%s', balance=%.2f, type='%s', active=%s}", 
                           accountNumber, accountHolderName, balance, accountType, isActive);
    }
    
    // Test the BankAccount class
    public static void main(String[] args) {
        System.out.println("=== Bank Account System Demo ===\n");
        
        // Create bank accounts
        BankAccount account1 = new BankAccount("John Doe", 1000.0, "Checking");
        BankAccount account2 = new BankAccount("Jane Smith", 500.0, "Savings");
        BankAccount account3 = new BankAccount(); // Default constructor
        
        // Print initial statements
        account1.printStatement();
        account2.printStatement();
        account3.printStatement();
        
        // Perform transactions
        System.out.println("=== Performing Transactions ===");
        account1.deposit(250.0);
        account1.withdraw(150.0);
        account2.withdraw(100.0);
        
        // Transfer money
        account1.transfer(account2, 200.0);
        
        // Add interest
        account2.addInterest(2.5); // 2.5% interest
        
        // Try invalid operations
        System.out.println("\n=== Testing Error Handling ===");
        account1.withdraw(2000.0); // Insufficient funds
        account1.deposit(-50.0);   // Negative deposit
        
        // Final statements
        System.out.println("\n=== Final Account Statements ===");
        account1.printStatement();
        account2.printStatement();
        
        // Close account
        account3.deposit(100.0);
        account3.withdraw(100.0);
        account3.closeAccount();
        account3.printStatement();
    }
}
```

---

## 🎯 Part 3: The `this` Keyword and Object References (15 minutes)

### **Understanding `this`**

Create `ThisKeywordDemo.java`:

```java
public class ThisKeywordDemo {
    
    // Instance variables
    private String name;
    private int value;
    
    // Constructor demonstrating this for parameter disambiguation
    public ThisKeywordDemo(String name, int value) {
        this.name = name;   // this.name refers to instance variable
        this.value = value; // this.value refers to instance variable
        // Without 'this', it would be ambiguous which 'name' and 'value' we're referring to
    }
    
    // Method demonstrating this for method chaining
    public ThisKeywordDemo setName(String name) {
        this.name = name;
        return this; // Return the current object for method chaining
    }
    
    public ThisKeywordDemo setValue(int value) {
        this.value = value;
        return this; // Return the current object for method chaining
    }
    
    // Method calling another method using this
    public void processData() {
        this.validateData(); // Explicitly call method on this object
        this.displayData();  // this is optional here, but good for clarity
    }
    
    private void validateData() {
        if (this.name == null || this.name.isEmpty()) {
            System.out.println("Warning: Name is empty");
        }
        if (this.value < 0) {
            System.out.println("Warning: Value is negative");
        }
    }
    
    private void displayData() {
        System.out.println("Name: " + this.name + ", Value: " + this.value);
    }
    
    // Constructor calling another constructor using this()
    public ThisKeywordDemo(String name) {
        this(name, 0); // Call the other constructor with default value
        System.out.println("Single-parameter constructor called");
    }
    
    public ThisKeywordDemo() {
        this("Default", 100); // Call the two-parameter constructor
        System.out.println("Default constructor called");
    }
    
    // Method demonstrating this as a parameter
    public void compareWith(ThisKeywordDemo other) {
        System.out.println("Comparing this object with another:");
        System.out.println("This: " + this.toString());
        System.out.println("Other: " + other.toString());
        
        if (this.value > other.value) {
            System.out.println("This object has higher value");
        } else if (this.value < other.value) {
            System.out.println("Other object has higher value");
        } else {
            System.out.println("Both objects have same value");
        }
    }
    
    // Method that returns this object
    public ThisKeywordDemo copy() {
        return new ThisKeywordDemo(this.name, this.value);
    }
    
    @Override
    public String toString() {
        return String.format("ThisKeywordDemo{name='%s', value=%d}", this.name, this.value);
    }
    
    public static void main(String[] args) {
        System.out.println("=== This Keyword Demo ===\n");
        
        // Constructor chaining demonstration
        System.out.println("1. Constructor Chaining:");
        ThisKeywordDemo obj1 = new ThisKeywordDemo();
        System.out.println();
        
        ThisKeywordDemo obj2 = new ThisKeywordDemo("Custom Name");
        System.out.println();
        
        // Method chaining demonstration
        System.out.println("2. Method Chaining:");
        ThisKeywordDemo obj3 = new ThisKeywordDemo("Initial", 50)
                                .setName("Chained")
                                .setValue(200);
        obj3.displayData();
        System.out.println();
        
        // Object comparison using this
        System.out.println("3. Object Comparison:");
        ThisKeywordDemo obj4 = new ThisKeywordDemo("Object4", 300);
        obj3.compareWith(obj4);
        System.out.println();
        
        // Process data with validation
        System.out.println("4. Method Calling with this:");
        obj3.processData();
        System.out.println();
        
        // Copy object
        System.out.println("5. Object Copying:");
        ThisKeywordDemo copy = obj3.copy();
        System.out.println("Original: " + obj3);
        System.out.println("Copy: " + copy);
    }
}
```

---

## 🎯 Today's Major Challenges

### **Challenge 1: Library Book Management System**

Create `Book.java`:

```java
public class Book {
    // Private instance variables
    private String isbn;
    private String title;
    private String author;
    private String genre;
    private int publicationYear;
    private double price;
    private boolean isAvailable;
    private String borrowedBy;
    private int timesCheckedOut;
    
    // Static variable to track total books
    private static int totalBooks = 0;
    
    // Default constructor
    public Book() {
        this.isbn = "000-0000000000";
        this.title = "Unknown Title";
        this.author = "Unknown Author";
        this.genre = "Fiction";
        this.publicationYear = 2000;
        this.price = 0.0;
        this.isAvailable = true;
        this.borrowedBy = null;
        this.timesCheckedOut = 0;
        totalBooks++;
        System.out.println("Default book created");
    }
    
    // Parameterized constructor
    public Book(String isbn, String title, String author, String genre, 
               int publicationYear, double price) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.publicationYear = publicationYear;
        this.price = price;
        this.isAvailable = true;
        this.borrowedBy = null;
        this.timesCheckedOut = 0;
        totalBooks++;
        System.out.println("Book created: " + title + " by " + author);
    }
    
    // Copy constructor
    public Book(Book other) {
        this.isbn = other.isbn + "_COPY";
        this.title = other.title;
        this.author = other.author;
        this.genre = other.genre;
        this.publicationYear = other.publicationYear;
        this.price = other.price;
        this.isAvailable = true;
        this.borrowedBy = null;
        this.timesCheckedOut = 0;
        totalBooks++;
        System.out.println("Copy of book created: " + title);
    }
    
    // Check out book
    public boolean checkOut(String borrowerName) {
        if (!this.isAvailable) {
            System.out.println("Book '" + this.title + "' is already checked out by " + this.borrowedBy);
            return false;
        }
        
        this.isAvailable = false;
        this.borrowedBy = borrowerName;
        this.timesCheckedOut++;
        System.out.println("Book '" + this.title + "' checked out to " + borrowerName);
        return true;
    }
    
    // Return book
    public boolean returnBook() {
        if (this.isAvailable) {
            System.out.println("Book '" + this.title + "' is already available");
            return false;
        }
        
        System.out.println("Book '" + this.title + "' returned by " + this.borrowedBy);
        this.isAvailable = true;
        this.borrowedBy = null;
        return true;
    }
    
    // Apply discount
    public void applyDiscount(double discountPercentage) {
        if (discountPercentage > 0 && discountPercentage <= 50) {
            double oldPrice = this.price;
            this.price = this.price * (1 - discountPercentage / 100);
            System.out.printf("Discount applied to '%s': $%.2f -> $%.2f (%.1f%% off)%n", 
                             this.title, oldPrice, this.price, discountPercentage);
        } else {
            System.out.println("Invalid discount percentage. Must be between 0 and 50.");
        }
    }
    
    // Check if book is popular (checked out more than 5 times)
    public boolean isPopular() {
        return this.timesCheckedOut > 5;
    }
    
    // Get book age
    public int getBookAge() {
        return 2025 - this.publicationYear;
    }
    
    // Display book info
    public void displayInfo() {
        System.out.println("=== Book Information ===");
        System.out.println("ISBN: " + this.isbn);
        System.out.println("Title: " + this.title);
        System.out.println("Author: " + this.author);
        System.out.println("Genre: " + this.genre);
        System.out.println("Publication Year: " + this.publicationYear);
        System.out.printf("Price: $%.2f%n", this.price);
        System.out.println("Available: " + (this.isAvailable ? "Yes" : "No (borrowed by " + this.borrowedBy + ")"));
        System.out.println("Times Checked Out: " + this.timesCheckedOut);
        System.out.println("Popular Book: " + (this.isPopular() ? "Yes" : "No"));
        System.out.println("Book Age: " + this.getBookAge() + " years");
        System.out.println();
    }
    
    // Static method to get total books
    public static int getTotalBooks() {
        return totalBooks;
    }
    
    // Getters
    public String getIsbn() { return this.isbn; }
    public String getTitle() { return this.title; }
    public String getAuthor() { return this.author; }
    public String getGenre() { return this.genre; }
    public int getPublicationYear() { return this.publicationYear; }
    public double getPrice() { return this.price; }
    public boolean isAvailable() { return this.isAvailable; }
    public String getBorrowedBy() { return this.borrowedBy; }
    public int getTimesCheckedOut() { return this.timesCheckedOut; }
    
    // Setters with validation
    public void setTitle(String title) {
        if (title != null && !title.trim().isEmpty()) {
            this.title = title;
        } else {
            System.out.println("Invalid title provided");
        }
    }
    
    public void setPrice(double price) {
        if (price >= 0) {
            this.price = price;
        } else {
            System.out.println("Price cannot be negative");
        }
    }
    
    @Override
    public String toString() {
        return String.format("Book{isbn='%s', title='%s', author='%s', available=%s}", 
                           isbn, title, author, isAvailable);
    }
    
    // Test the Book class
    public static void main(String[] args) {
        System.out.println("=== Library Book Management System ===\n");
        
        // Create books
        Book book1 = new Book("978-0134685991", "Effective Java", "Joshua Bloch", 
                             "Programming", 2017, 45.99);
        
        Book book2 = new Book("978-0132350884", "Clean Code", "Robert Martin", 
                             "Programming", 2008, 42.50);
        
        Book book3 = new Book("978-0201633610", "Design Patterns", "Gang of Four", 
                             "Programming", 1994, 54.95);
        
        Book book4 = new Book(); // Default book
        
        System.out.println("Total books in system: " + Book.getTotalBooks());
        System.out.println();
        
        // Display book information
        book1.displayInfo();
        book2.displayInfo();
        
        // Test checking out books
        System.out.println("=== Testing Book Operations ===");
        book1.checkOut("Alice Johnson");
        book1.checkOut("Bob Smith"); // Should fail - already checked out
        
        book2.checkOut("Charlie Brown");
        book2.returnBook();
        book2.checkOut("Diana Prince");
        
        // Simulate multiple checkouts for popularity test
        for (int i = 0; i < 7; i++) {
            book3.checkOut("User" + i);
            book3.returnBook();
        }
        
        // Apply discounts
        book2.applyDiscount(15.0);
        book3.applyDiscount(60.0); // Invalid discount
        
        // Final display
        System.out.println("\n=== Final Book Status ===");
        book1.displayInfo();
        book2.displayInfo();
        book3.displayInfo();
        
        // Create a copy
        Book bookCopy = new Book(book1);
        bookCopy.displayInfo();
        
        System.out.println("Total books in system: " + Book.getTotalBooks());
    }
}
```

### **Challenge 2: Car Dealership System**

Create `Car.java`:

```java
public class Car {
    // Private instance variables
    private String make;
    private String model;
    private int year;
    private String color;
    private double price;
    private int mileage;
    private String fuelType;
    private boolean isAvailable;
    private String vin; // Vehicle Identification Number
    
    // Static variables
    private static int totalCars = 0;
    private static int nextVinNumber = 10001;
    
    // Default constructor
    public Car() {
        this.make = "Unknown";
        this.model = "Unknown";
        this.year = 2020;
        this.color = "White";
        this.price = 20000.0;
        this.mileage = 0;
        this.fuelType = "Gasoline";
        this.isAvailable = true;
        this.vin = generateVin();
        totalCars++;
        System.out.println("Default car created with VIN: " + this.vin);
    }
    
    // Parameterized constructor
    public Car(String make, String model, int year, String color, 
               double price, int mileage, String fuelType) {
        this.make = make;
        this.model = model;
        this.year = year;
        this.color = color;
        this.price = price;
        this.mileage = mileage;
        this.fuelType = fuelType;
        this.isAvailable = true;
        this.vin = generateVin();
        totalCars++;
        System.out.println("Car created: " + year + " " + make + " " + model + " (VIN: " + this.vin + ")");
    }
    
    // Overloaded constructor for new cars (0 mileage)
    public Car(String make, String model, int year, String color, double price) {
        this(make, model, year, color, price, 0, "Gasoline");
        System.out.println("New car (0 miles) created");
    }
    
    // Generate unique VIN
    private static String generateVin() {
        return "VIN" + nextVinNumber++;
    }
    
    // Sell the car
    public boolean sellCar(String buyerName) {
        if (!this.isAvailable) {
            System.out.println("Car " + this.vin + " is not available for sale");
            return false;
        }
        
        this.isAvailable = false;
        System.out.println("Car sold to " + buyerName + ": " + this.year + " " + 
                          this.make + " " + this.model + " for $" + String.format("%.2f", this.price));
        return true;
    }
    
    // Add mileage (for used cars)
    public void addMileage(int miles) {
        if (miles > 0) {
            this.mileage += miles;
            this.updatePriceBasedOnMileage();
            System.out.println("Added " + miles + " miles. Total mileage: " + this.mileage);
        } else {
            System.out.println("Miles to add must be positive");
        }
    }
    
    // Update price based on mileage (depreciation)
    private void updatePriceBasedOnMileage() {
        // Depreciate $0.10 per mile for luxury cars, $0.05 for others
        double depreciationRate = this.isLuxuryCar() ? 0.10 : 0.05;
        double basePrice = this.price + (this.mileage * depreciationRate);
        this.price = Math.max(basePrice - (this.mileage * depreciationRate), this.price * 0.3); // Min 30% of original
    }
    
    // Check if luxury car
    public boolean isLuxuryCar() {
        String luxuryBrands = "BMW,Mercedes,Audi,Lexus,Porsche,Jaguar";
        return luxuryBrands.toLowerCase().contains(this.make.toLowerCase());
    }
    
    // Calculate car age
    public int getCarAge() {
        return 2025 - this.year;
    }
    
    // Check if car is vintage (25+ years old)
    public boolean isVintage() {
        return this.getCarAge() >= 25;
    }
    
    // Apply discount
    public void applyDiscount(double discountPercentage, String reason) {
        if (discountPercentage > 0 && discountPercentage <= 30) {
            double oldPrice = this.price;
            this.price = this.price * (1 - discountPercentage / 100);
            System.out.printf("Discount applied to %s %s %s: $%.2f -> $%.2f (%.1f%% off - %s)%n", 
                             this.year, this.make, this.model, oldPrice, this.price, 
                             discountPercentage, reason);
        } else {
            System.out.println("Invalid discount percentage. Must be between 0 and 30.");
        }
    }
    
    // Service the car (reset to available if serviced)
    public void serviceCar() {
        System.out.println("Servicing car: " + this.year + " " + this.make + " " + this.model);
        // If car was sold, make it available again (trade-in scenario)
        if (!this.isAvailable) {
            this.isAvailable = true;
            System.out.println("Car is now available again after service");
        }
    }
    
    // Display car information
    public void displayInfo() {
        System.out.println("=== Car Information ===");
        System.out.println("VIN: " + this.vin);
        System.out.println("Make: " + this.make);
        System.out.println("Model: " + this.model);
        System.out.println("Year: " + this.year);
        System.out.println("Color: " + this.color);
        System.out.printf("Price: $%.2f%n", this.price);
        System.out.println("Mileage: " + String.format("%,d", this.mileage) + " miles");
        System.out.println("Fuel Type: " + this.fuelType);
        System.out.println("Available: " + (this.isAvailable ? "Yes" : "Sold"));
        System.out.println("Car Age: " + this.getCarAge() + " years");
        System.out.println("Luxury Car: " + (this.isLuxuryCar() ? "Yes" : "No"));
        System.out.println("Vintage Car: " + (this.isVintage() ? "Yes" : "No"));
        System.out.println();
    }
    
    // Compare with another car
    public void compareWith(Car other) {
        System.out.println("=== Car Comparison ===");
        System.out.println("This car: " + this.year + " " + this.make + " " + this.model + 
                          " - $" + String.format("%.2f", this.price));
        System.out.println("Other car: " + other.year + " " + other.make + " " + other.model + 
                          " - $" + String.format("%.2f", other.price));
        
        if (this.price > other.price) {
            System.out.println("This car is more expensive");
        } else if (this.price < other.price) {
            System.out.println("Other car is more expensive");
        } else {
            System.out.println("Both cars have the same price");
        }
        
        if (this.mileage < other.mileage) {
            System.out.println("This car has lower mileage");
        } else if (this.mileage > other.mileage) {
            System.out.println("Other car has lower mileage");
        } else {
            System.out.println("Both cars have the same mileage");
        }
        System.out.println();
    }
    
    // Static methods
    public static int getTotalCars() {
        return totalCars;
    }
    
    public static void displayDealershipStats() {
        System.out.println("=== Dealership Statistics ===");
        System.out.println("Total cars in inventory: " + totalCars);
        System.out.println("Next VIN number: VIN" + nextVinNumber);
        System.out.println();
    }
    
    // Getters and Setters
    public String getMake() { return this.make; }
    public String getModel() { return this.model; }
    public int getYear() { return this.year; }
    public String getColor() { return this.color; }
    public double getPrice() { return this.price; }
    public int getMileage() { return this.mileage; }
    public String getFuelType() { return this.fuelType; }
    public boolean isAvailable() { return this.isAvailable; }
    public String getVin() { return this.vin; }
    
    @Override
    public String toString() {
        return String.format("Car{vin='%s', %d %s %s, $%.2f, available=%s}", 
                           vin, year, make, model, price, isAvailable);
    }
    
    // Test the Car class
    public static void main(String[] args) {
        System.out.println("=== Car Dealership Management System ===\n");
        
        // Create different types of cars
        Car car1 = new Car("Toyota", "Camry", 2022, "Silver", 28500.0);
        Car car2 = new Car("BMW", "X5", 2021, "Black", 65000.0, 15000, "Gasoline");
        Car car3 = new Car("Tesla", "Model 3", 2023, "White", 45000.0, 5000, "Electric");
        Car car4 = new Car("Ford", "Mustang", 1965, "Red", 45000.0, 80000, "Gasoline");
        Car car5 = new Car(); // Default car
        
        Car.displayDealershipStats();
        
        // Display car information
        car1.displayInfo();
        car2.displayInfo();
        car4.displayInfo();
        
        // Test car operations
        System.out.println("=== Testing Car Operations ===");
        car1.sellCar("John Smith");
        car2.addMileage(5000);
        car3.applyDiscount(10.0, "End of year sale");
        
        // Compare cars
        car2.compareWith(car3);
        
        // Service and make available again
        car1.serviceCar();
        
        // Final status
        System.out.println("=== Final Car Status ===");
        car1.displayInfo();
        car2.displayInfo();
        car3.displayInfo();
        
        Car.displayDealershipStats();
    }
}
```

---

## 📝 Key Takeaways from Day 5

### **Class and Object Fundamentals:**
- ✅ **Class structure:** Instance variables, methods, constructors
- ✅ **Object creation:** Using different constructors
- ✅ **Encapsulation:** Private variables with public methods
- ✅ **this keyword:** Disambiguation, method chaining, constructor calling

### **Constructor Mastery:**
- ✅ **Default constructors:** No-parameter initialization
- ✅ **Parameterized constructors:** Flexible object creation
- ✅ **Constructor overloading:** Multiple ways to create objects
- ✅ **Constructor chaining:** Using this() to call other constructors

### **Object-Oriented Design:**
- ✅ **Real-world modeling:** Students, bank accounts, books, cars
- ✅ **State and behavior:** What objects know and what they can do
- ✅ **Object interactions:** Objects working together
- ✅ **Static vs instance:** Class-level vs object-level members

### **Best Practices:**
- ✅ **Validation:** Checking inputs in setters and methods
- ✅ **toString method:** Easy object representation
- ✅ **Method organization:** Logical grouping of functionality
- ✅ **Documentation:** Clear method and class purposes

---

## 🚀 Tomorrow's Preview (Day 6 - July 15)

**Focus:** Encapsulation & Data Hiding
- Getters and setters in detail
- Data validation in setters
- Immutable classes
- Package organization and import statements
- Access modifier best practices

---

## ✅ Day 5 Checklist

**Completed Tasks:**
- [ ] Create custom classes with instance variables and methods
- [ ] Master different types of constructors (default, parameterized, overloaded)
- [ ] Use the `this` keyword for disambiguation and method chaining
- [ ] Implement proper access modifiers (private, public)
- [ ] Build Library Book Management System
- [ ] Build Car Dealership Management System
- [ ] Design objects that model real-world entities
- [ ] Commit code to GitHub: "Day 5: Classes and Objects - OOP Foundation"

**Bonus Achievements:**
- [ ] Add more features to the Book class (reservations, reviews)
- [ ] Extend the Car class with additional functionality
- [ ] Create additional classes (Customer, Library, Dealership)
- [ ] Experiment with object relationships

---

## 🎉 Major Milestone Achieved!

Congratulations! You've just entered the world of Object-Oriented Programming! You can now:
- Model real-world entities as Java classes
- Create objects with state and behavior
- Design flexible constructors for object creation
- Use encapsulation to protect data integrity

Tomorrow we'll dive deeper into encapsulation and learn how to organize code into packages. You're building the skills of a professional Java developer!

**Fantastic progress! You're thinking like an object-oriented programmer now!** 🚀