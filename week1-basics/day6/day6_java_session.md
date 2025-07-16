# Day 6: Encapsulation & Data Hiding - Building Secure Classes
**Date:** July 15, 2025  
**Duration:** 1.5 hours (Combined Session)  
**Focus:** Getters/setters, data validation, immutable classes, and package organization

---

## 🎯 Today's Learning Goals
By the end of this session, you'll:
- ✅ Master getters and setters with proper validation
- ✅ Understand all access modifiers (private, public, protected, default)
- ✅ Create immutable classes for thread-safe programming
- ✅ Organize code with packages and imports
- ✅ Implement data validation and error handling
- ✅ Build secure and maintainable class designs
- ✅ Apply encapsulation best practices

---

## 📚 Part 1: Access Modifiers & Encapsulation (20 minutes)

### **Understanding Access Levels**

Create `AccessModifiersDemo.java`:

```java
// Package declaration (if organizing in packages)
// package com.javalearning.encapsulation;

public class AccessModifiersDemo {
    
    // Private - only accessible within this class
    private String privateData = "Private information";
    
    // Default (package-private) - accessible within same package
    String defaultData = "Package-private information";
    
    // Protected - accessible within package and subclasses
    protected String protectedData = "Protected information";
    
    // Public - accessible from anywhere
    public String publicData = "Public information";
    
    // Private method - only callable within this class
    private void privateMethod() {
        System.out.println("This is a private method");
        System.out.println("Can access: " + privateData);
    }
    
    // Default method - accessible within same package
    void defaultMethod() {
        System.out.println("This is a default (package-private) method");
        privateMethod(); // Can call private methods within same class
    }
    
    // Protected method - accessible within package and subclasses
    protected void protectedMethod() {
        System.out.println("This is a protected method");
    }
    
    // Public method - accessible from anywhere
    public void publicMethod() {
        System.out.println("This is a public method");
        System.out.println("Can access all data in this class:");
        System.out.println("Private: " + privateData);
        System.out.println("Default: " + defaultData);
        System.out.println("Protected: " + protectedData);
        System.out.println("Public: " + publicData);
    }
    
    // Demonstration of access within same class
    public void demonstrateAccess() {
        System.out.println("=== Access Within Same Class ===");
        // Can access all members within the same class
        System.out.println("Accessing private data: " + this.privateData);
        this.privateMethod();
        this.defaultMethod();
        this.protectedMethod();
        this.publicMethod();
        System.out.println();
    }
    
    public static void main(String[] args) {
        System.out.println("=== Access Modifiers Demo ===\n");
        
        AccessModifiersDemo demo = new AccessModifiersDemo();
        demo.demonstrateAccess();
        
        // Within same class, can access all members
        System.out.println("=== Direct Access in Main Method ===");
        System.out.println("Private: " + demo.privateData);     // ✅ Accessible
        System.out.println("Default: " + demo.defaultData);     // ✅ Accessible
        System.out.println("Protected: " + demo.protectedData); // ✅ Accessible
        System.out.println("Public: " + demo.publicData);       // ✅ Accessible
        
        demo.privateMethod();    // ✅ Accessible
        demo.defaultMethod();    // ✅ Accessible
        demo.protectedMethod();  // ✅ Accessible
        demo.publicMethod();     // ✅ Accessible
    }
}

// Another class in the same file to demonstrate access
class SamePackageClass {
    public void testAccess() {
        AccessModifiersDemo demo = new AccessModifiersDemo();
        
        System.out.println("=== Access from Same Package Class ===");
        // System.out.println(demo.privateData);    // ❌ NOT accessible
        System.out.println("Default: " + demo.defaultData);     // ✅ Accessible
        System.out.println("Protected: " + demo.protectedData); // ✅ Accessible
        System.out.println("Public: " + demo.publicData);       // ✅ Accessible
        
        // demo.privateMethod();    // ❌ NOT accessible
        demo.defaultMethod();       // ✅ Accessible
        demo.protectedMethod();     // ✅ Accessible
        demo.publicMethod();        // ✅ Accessible
    }
}
```

---

## 🔒 Part 2: Proper Getters and Setters (25 minutes)

### **Employee Class with Validation**

Create `Employee.java`:

```java
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

public class Employee {
    
    // Private instance variables (encapsulated data)
    private String employeeId;
    private String firstName;
    private String lastName;
    private String email;
    private String department;
    private String position;
    private double salary;
    private LocalDate hireDate;
    private LocalDate birthDate;
    private boolean isActive;
    private List<String> skills;
    
    // Static variable for generating employee IDs
    private static int nextEmployeeNumber = 1001;
    
    // Constants
    private static final double MIN_SALARY = 30000.0;
    private static final double MAX_SALARY = 500000.0;
    
    // Constructor
    public Employee(String firstName, String lastName, String email, 
                   String department, String position, double salary, LocalDate birthDate) {
        this.employeeId = generateEmployeeId();
        this.setFirstName(firstName);
        this.setLastName(lastName);
        this.setEmail(email);
        this.setDepartment(department);
        this.setPosition(position);
        this.setSalary(salary);
        this.setBirthDate(birthDate);
        this.hireDate = LocalDate.now();
        this.isActive = true;
        this.skills = new ArrayList<>();
        
        System.out.println("Employee created: " + this.getFullName() + " (ID: " + this.employeeId + ")");
    }
    
    // Generate unique employee ID
    private static String generateEmployeeId() {
        return "EMP" + String.format("%04d", nextEmployeeNumber++);
    }
    
    // GETTERS (Accessors) - Provide controlled access to private data
    
    public String getEmployeeId() {
        return this.employeeId;
    }
    
    public String getFirstName() {
        return this.firstName;
    }
    
    public String getLastName() {
        return this.lastName;
    }
    
    public String getFullName() {
        return this.firstName + " " + this.lastName;
    }
    
    public String getEmail() {
        return this.email;
    }
    
    public String getDepartment() {
        return this.department;
    }
    
    public String getPosition() {
        return this.position;
    }
    
    public double getSalary() {
        return this.salary;
    }
    
    // Formatted salary getter
    public String getFormattedSalary() {
        return String.format("$%,.2f", this.salary);
    }
    
    public LocalDate getHireDate() {
        return this.hireDate; // LocalDate is immutable, safe to return directly
    }
    
    public LocalDate getBirthDate() {
        return this.birthDate;
    }
    
    public boolean isActive() {
        return this.isActive;
    }
    
    // Return a copy of the skills list to prevent external modification
    public List<String> getSkills() {
        return new ArrayList<>(this.skills);
    }
    
    // Computed properties (calculated getters)
    public int getAge() {
        return Period.between(this.birthDate, LocalDate.now()).getYears();
    }
    
    public int getYearsOfService() {
        return Period.between(this.hireDate, LocalDate.now()).getYears();
    }
    
    public boolean isSeniorEmployee() {
        return this.getYearsOfService() >= 5;
    }
    
    // SETTERS (Mutators) - Provide controlled modification of private data
    
    public void setFirstName(String firstName) {
        if (firstName == null || firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name cannot be null or empty");
        }
        if (firstName.trim().length() < 2) {
            throw new IllegalArgumentException("First name must be at least 2 characters");
        }
        if (!firstName.matches("[a-zA-Z\\s'-]+")) {
            throw new IllegalArgumentException("First name can only contain letters, spaces, hyphens, and apostrophes");
        }
        this.firstName = capitalizeFirstLetter(firstName.trim());
    }
    
    public void setLastName(String lastName) {
        if (lastName == null || lastName.trim().isEmpty()) {
            throw new IllegalArgumentException("Last name cannot be null or empty");
        }
        if (lastName.trim().length() < 2) {
            throw new IllegalArgumentException("Last name must be at least 2 characters");
        }
        if (!lastName.matches("[a-zA-Z\\s'-]+")) {
            throw new IllegalArgumentException("Last name can only contain letters, spaces, hyphens, and apostrophes");
        }
        this.lastName = capitalizeFirstLetter(lastName.trim());
    }
    
    public void setEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        email = email.trim().toLowerCase();
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format");
        }
        this.email = email;
    }
    
    public void setDepartment(String department) {
        if (department == null || department.trim().isEmpty()) {
            throw new IllegalArgumentException("Department cannot be null or empty");
        }
        
        // Validate against allowed departments
        String[] validDepartments = {"Engineering", "Marketing", "Sales", "HR", "Finance", "Operations", "IT"};
        String normalizedDept = capitalizeFirstLetter(department.trim());
        
        boolean isValid = false;
        for (String validDept : validDepartments) {
            if (validDept.equalsIgnoreCase(normalizedDept)) {
                this.department = validDept;
                isValid = true;
                break;
            }
        }
        
        if (!isValid) {
            throw new IllegalArgumentException("Invalid department. Must be one of: " + 
                                             String.join(", ", validDepartments));
        }
    }
    
    public void setPosition(String position) {
        if (position == null || position.trim().isEmpty()) {
            throw new IllegalArgumentException("Position cannot be null or empty");
        }
        this.position = capitalizeWords(position.trim());
    }
    
    public void setSalary(double salary) {
        if (salary < MIN_SALARY) {
            throw new IllegalArgumentException("Salary cannot be less than " + 
                                             String.format("$%,.2f", MIN_SALARY));
        }
        if (salary > MAX_SALARY) {
            throw new IllegalArgumentException("Salary cannot exceed " + 
                                             String.format("$%,.2f", MAX_SALARY));
        }
        this.salary = salary;
    }
    
    public void setBirthDate(LocalDate birthDate) {
        if (birthDate == null) {
            throw new IllegalArgumentException("Birth date cannot be null");
        }
        
        LocalDate now = LocalDate.now();
        int age = Period.between(birthDate, now).getYears();
        
        if (age < 18) {
            throw new IllegalArgumentException("Employee must be at least 18 years old");
        }
        if (age > 80) {
            throw new IllegalArgumentException("Employee cannot be older than 80 years");
        }
        
        this.birthDate = birthDate;
    }
    
    public void setActive(boolean active) {
        if (this.isActive && !active) {
            System.out.println("Employee " + this.getFullName() + " has been deactivated");
        } else if (!this.isActive && active) {
            System.out.println("Employee " + this.getFullName() + " has been reactivated");
        }
        this.isActive = active;
    }
    
    // Business methods
    public void addSkill(String skill) {
        if (skill == null || skill.trim().isEmpty()) {
            throw new IllegalArgumentException("Skill cannot be null or empty");
        }
        
        String normalizedSkill = capitalizeWords(skill.trim());
        if (!this.skills.contains(normalizedSkill)) {
            this.skills.add(normalizedSkill);
            System.out.println("Skill added to " + this.getFullName() + ": " + normalizedSkill);
        } else {
            System.out.println(this.getFullName() + " already has skill: " + normalizedSkill);
        }
    }
    
    public boolean removeSkill(String skill) {
        String normalizedSkill = capitalizeWords(skill.trim());
        boolean removed = this.skills.remove(normalizedSkill);
        if (removed) {
            System.out.println("Skill removed from " + this.getFullName() + ": " + normalizedSkill);
        } else {
            System.out.println(this.getFullName() + " does not have skill: " + normalizedSkill);
        }
        return removed;
    }
    
    public void giveRaise(double percentage) {
        if (percentage <= 0) {
            throw new IllegalArgumentException("Raise percentage must be positive");
        }
        if (percentage > 50) {
            throw new IllegalArgumentException("Raise percentage cannot exceed 50%");
        }
        
        double oldSalary = this.salary;
        double newSalary = oldSalary * (1 + percentage / 100);
        
        // Check if new salary exceeds maximum
        if (newSalary > MAX_SALARY) {
            newSalary = MAX_SALARY;
            System.out.printf("Raise capped at maximum salary for %s%n", this.getFullName());
        }
        
        this.salary = newSalary;
        System.out.printf("Raise given to %s: %.1f%% increase from %s to %s%n", 
                         this.getFullName(), percentage, 
                         String.format("$%,.2f", oldSalary), this.getFormattedSalary());
    }
    
    // Utility methods
    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
    
    private String capitalizeWords(String str) {
        if (str == null || str.isEmpty()) return str;
        
        String[] words = str.split("\\s+");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < words.length; i++) {
            if (i > 0) result.append(" ");
            result.append(capitalizeFirstLetter(words[i]));
        }
        
        return result.toString();
    }
    
    private boolean isValidEmail(String email) {
        // Simple email validation regex
        return email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }
    
    // Display methods
    public void displayBasicInfo() {
        System.out.println("=== Employee Basic Information ===");
        System.out.println("ID: " + this.employeeId);
        System.out.println("Name: " + this.getFullName());
        System.out.println("Email: " + this.email);
        System.out.println("Department: " + this.department);
        System.out.println("Position: " + this.position);
        System.out.println("Status: " + (this.isActive ? "Active" : "Inactive"));
        System.out.println();
    }
    
    public void displayDetailedInfo() {
        System.out.println("=== Detailed Employee Information ===");
        System.out.println("ID: " + this.employeeId);
        System.out.println("Name: " + this.getFullName());
        System.out.println("Email: " + this.email);
        System.out.println("Department: " + this.department);
        System.out.println("Position: " + this.position);
        System.out.println("Salary: " + this.getFormattedSalary());
        System.out.println("Age: " + this.getAge() + " years");
        System.out.println("Hire Date: " + this.hireDate);
        System.out.println("Years of Service: " + this.getYearsOfService());
        System.out.println("Senior Employee: " + (this.isSeniorEmployee() ? "Yes" : "No"));
        System.out.println("Status: " + (this.isActive ? "Active" : "Inactive"));
        System.out.println("Skills: " + (this.skills.isEmpty() ? "None" : String.join(", ", this.skills)));
        System.out.println();
    }
    
    @Override
    public String toString() {
        return String.format("Employee{id='%s', name='%s', dept='%s', salary=%s, active=%s}", 
                           employeeId, getFullName(), department, getFormattedSalary(), isActive);
    }
    
    // Test the Employee class
    public static void main(String[] args) {
        System.out.println("=== Employee Management System Demo ===\n");
        
        try {
            // Create employees
            Employee emp1 = new Employee(
                "john", "doe", "john.doe@company.com", 
                "engineering", "software developer", 75000.0, 
                LocalDate.of(1990, 5, 15)
            );
            
            Employee emp2 = new Employee(
                "jane", "smith", "jane.smith@company.com",
                "marketing", "marketing manager", 85000.0,
                LocalDate.of(1985, 8, 20)
            );
            
            // Display initial information
            emp1.displayDetailedInfo();
            emp2.displayDetailedInfo();
            
            // Test business operations
            System.out.println("=== Testing Business Operations ===");
            emp1.addSkill("Java");
            emp1.addSkill("Spring Boot");
            emp1.addSkill("Database Design");
            emp1.addSkill("Java"); // Duplicate - should be ignored
            
            emp2.addSkill("Digital Marketing");
            emp2.addSkill("SEO");
            
            // Give raises
            emp1.giveRaise(10.0); // 10% raise
            emp2.giveRaise(7.5);  // 7.5% raise
            
            // Test validation
            System.out.println("\n=== Testing Validation ===");
            try {
                emp1.setSalary(-1000); // Should fail
            } catch (IllegalArgumentException e) {
                System.out.println("Validation caught: " + e.getMessage());
            }
            
            try {
                emp1.setEmail("invalid-email"); // Should fail
            } catch (IllegalArgumentException e) {
                System.out.println("Validation caught: " + e.getMessage());
            }
            
            try {
                emp1.setDepartment("Invalid Department"); // Should fail
            } catch (IllegalArgumentException e) {
                System.out.println("Validation caught: " + e.getMessage());
            }
            
            // Final display
            System.out.println("\n=== Final Employee Information ===");
            emp1.displayDetailedInfo();
            emp2.displayDetailedInfo();
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
```

---

## 🔐 Part 3: Immutable Classes (20 minutes)

### **Creating Thread-Safe Immutable Objects**

Create `ImmutablePerson.java`:

```java
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable Person class - once created, objects cannot be modified
 * This makes the class thread-safe and eliminates many bugs
 */
public final class ImmutablePerson { // final class - cannot be extended
    
    // All fields are private and final
    private final String firstName;
    private final String lastName;
    private final LocalDate birthDate;
    private final String email;
    private final List<String> hobbies; // Mutable object - need special handling
    
    // Constructor - only way to set values
    public ImmutablePerson(String firstName, String lastName, LocalDate birthDate, 
                          String email, List<String> hobbies) {
        // Validate inputs
        if (firstName == null || firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name cannot be null or empty");
        }
        if (lastName == null || lastName.trim().isEmpty()) {
            throw new IllegalArgumentException("Last name cannot be null or empty");
        }
        if (birthDate == null) {
            throw new IllegalArgumentException("Birth date cannot be null");
        }
        if (email == null || !isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email address");
        }
        
        // Store immutable copies of the data
        this.firstName = firstName.trim();
        this.lastName = lastName.trim();
        this.birthDate = birthDate; // LocalDate is already immutable
        this.email = email.trim().toLowerCase();
        
        // Create defensive copy of mutable list
        this.hobbies = hobbies == null ? 
            new ArrayList<>() : 
            new ArrayList<>(hobbies);
    }
    
    // Convenience constructor without hobbies
    public ImmutablePerson(String firstName, String lastName, LocalDate birthDate, String email) {
        this(firstName, lastName, birthDate, email, null);
    }
    
    // All getters - no setters!
    public String getFirstName() {
        return this.firstName;
    }
    
    public String getLastName() {
        return this.lastName;
    }
    
    public String getFullName() {
        return this.firstName + " " + this.lastName;
    }
    
    public LocalDate getBirthDate() {
        return this.birthDate; // LocalDate is immutable, safe to return
    }
    
    public String getEmail() {
        return this.email;
    }
    
    // Return unmodifiable view of hobbies list
    public List<String> getHobbies() {
        return Collections.unmodifiableList(this.hobbies);
    }
    
    // Computed properties
    public int getAge() {
        return LocalDate.now().getYear() - this.birthDate.getYear();
    }
    
    // Methods that return NEW objects instead of modifying this one
    public ImmutablePerson withFirstName(String newFirstName) {
        return new ImmutablePerson(newFirstName, this.lastName, this.birthDate, 
                                  this.email, this.hobbies);
    }
    
    public ImmutablePerson withLastName(String newLastName) {
        return new ImmutablePerson(this.firstName, newLastName, this.birthDate, 
                                  this.email, this.hobbies);
    }
    
    public ImmutablePerson withEmail(String newEmail) {
        return new ImmutablePerson(this.firstName, this.lastName, this.birthDate, 
                                  newEmail, this.hobbies);
    }
    
    public ImmutablePerson withAdditionalHobby(String hobby) {
        List<String> newHobbies = new ArrayList<>(this.hobbies);
        if (!newHobbies.contains(hobby)) {
            newHobbies.add(hobby);
        }
        return new ImmutablePerson(this.firstName, this.lastName, this.birthDate, 
                                  this.email, newHobbies);
    }
    
    public ImmutablePerson withoutHobby(String hobby) {
        List<String> newHobbies = new ArrayList<>(this.hobbies);
        newHobbies.remove(hobby);
        return new ImmutablePerson(this.firstName, this.lastName, this.birthDate, 
                                  this.email, newHobbies);
    }
    
    // Utility methods
    private static boolean isValidEmail(String email) {
        return email != null && email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }
    
    // equals and hashCode for proper object comparison
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        ImmutablePerson that = (ImmutablePerson) obj;
        return this.firstName.equals(that.firstName) &&
               this.lastName.equals(that.lastName) &&
               this.birthDate.equals(that.birthDate) &&
               this.email.equals(that.email) &&
               this.hobbies.equals(that.hobbies);
    }
    
    @Override
    public int hashCode() {
        int result = firstName.hashCode();
        result = 31 * result + lastName.hashCode();
        result = 31 * result + birthDate.hashCode();
        result = 31 * result + email.hashCode();
        result = 31 * result + hobbies.hashCode();
        return result;
    }
    
    @Override
    public String toString() {
        return String.format("ImmutablePerson{name='%s', birthDate=%s, email='%s', hobbies=%s}", 
                           getFullName(), birthDate, email, hobbies);
    }
    
    // Test the immutable class
    public static void main(String[] args) {
        System.out.println("=== Immutable Person Demo ===\n");
        
        // Create hobbies list
        List<String> hobbies = new ArrayList<>();
        hobbies.add("Reading");
        hobbies.add("Swimming");
        hobbies.add("Programming");
        
        // Create immutable person
        ImmutablePerson person1 = new ImmutablePerson(
            "John", "Doe", LocalDate.of(1990, 5, 15), 
            "john.doe@email.com", hobbies
        );
        
        System.out.println("Original person: " + person1);
        System.out.println("Age: " + person1.getAge());
        System.out.println("Hobbies: " + person1.getHobbies());
        System.out.println();
        
        // Try to modify hobbies list (should not affect original)
        hobbies.add("Gaming"); // This won't affect person1
        System.out.println("After adding 'Gaming' to original list:");
        System.out.println("Person hobbies: " + person1.getHobbies()); // Still original hobbies
        System.out.println();
        
        // Try to modify returned hobbies list (should fail)
        try {
            person1.getHobbies().add("Cooking"); // This will throw exception
        } catch (UnsupportedOperationException e) {
            System.out.println("Cannot modify returned hobbies list: " + e.getClass().getSimpleName());
        }
        System.out.println();
        
        // Create new objects using "with" methods
        System.out.println("=== Creating Modified Copies ===");
        ImmutablePerson person2 = person1.withFirstName("Jane");
        ImmutablePerson person3 = person1.withAdditionalHobby("Cooking");
        ImmutablePerson person4 = person1.withEmail("john.doe.new@email.com");
        
        System.out.println("Person 1 (original): " + person1);
        System.out.println("Person 2 (new first name): " + person2);
        System.out.println("Person 3 (added hobby): " + person3);
        System.out.println("Person 4 (new email): " + person4);
        System.out.println();
        
        // Demonstrate thread safety
        System.out.println("=== Thread Safety Demo ===");
        final ImmutablePerson sharedPerson = person1;
        
        // Multiple threads can safely access immutable object
        for (int i = 0; i < 3; i++) {
            final int threadNum = i + 1;
            Thread thread = new Thread(() -> {
                System.out.println("Thread " + threadNum + " reading: " + sharedPerson.getFullName());
                System.out.println("Thread " + threadNum + " hobbies: " + sharedPerson.getHobbies().size());
                
                // Each thread can create its own modified version
                ImmutablePerson modified = sharedPerson.withAdditionalHobby("Thread" + threadNum + "Hobby");
                System.out.println("Thread " + threadNum + " created: " + modified.getHobbies());
            });
            thread.start();
        }
        
        // Wait a moment for threads to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("\nOriginal person unchanged: " + sharedPerson);
        
        // Test equality
        System.out.println("\n=== Equality Testing ===");
        ImmutablePerson person5 = new ImmutablePerson(
            "John", "Doe", LocalDate.of(1990, 5, 15), 
            "john.doe@email.com", List.of("Reading", "Swimming", "Programming")
        );
        
        System.out.println("person1.equals(person5): " + person1.equals(person5));
        System.out.println("person1.hashCode(): " + person1.hashCode());
        System.out.println("person5.hashCode(): " + person5.hashCode());
    }
}
```

---

## 📦 Part 4: Package Organization (15 minutes)

### **Creating Package Structure**

Create the following package structure and files:

**File 1: `com/javalearning/model/Product.java`**

```java
package com.javalearning.model;

import java.time.LocalDateTime;

/**
 * Product class representing items in an inventory system
 */
public class Product {
    private String productId;
    private String name;
    private String category;
    private double price;
    private int stockQuantity;
    private LocalDateTime createdDate;
    
    // Package-private constructor (accessible within same package)
    Product(String productId, String name, String category, double price, int stockQuantity) {
        this.productId = productId;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.createdDate = LocalDateTime.now();
    }
    
    // Public getters
    public String getProductId() { return productId; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public double getPrice() { return price; }
    public int getStockQuantity() { return stockQuantity; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    
    // Package-private setters (controlled access)
    void setPrice(double price) {
        if (price >= 0) {
            this.price = price;
        }
    }
    
    void updateStock(int quantity) {
        this.stockQuantity = Math.max(0, this.stockQuantity + quantity);
    }
    
    // Protected method for subclasses
    protected void setCategory(String category) {
        this.category = category;
    }
    
    @Override
    public String toString() {
        return String.format("Product{id='%s', name='%s', price=%.2f, stock=%d}", 
                           productId, name, price, stockQuantity);
    }
}
```

**File 2: `com/javalearning/service/ProductService.java`**

```java
package com.javalearning.service;

import com.javalearning.model.Product;
import java.util.*;

/**
 * Service class for managing products - demonstrates package access
 */
public class ProductService {
    private List<Product> products;
    private static int nextProductId = 1001;
    
    public ProductService() {
        this.products = new ArrayList<>();
    }
    
    // Public method to create products
    public Product createProduct(String name, String category, double price, int stockQuantity) {
        String productId = "PROD" + String.format("%04d", nextProductId++);
        
        // Can use package-private constructor since we import the model
        Product product = new Product(productId, name, category, price, stockQuantity);
        products.add(product);
        
        System.out.println("Product created: " + product);
        return product;
    }
    
    public void updateProductPrice(String productId, double newPrice) {
        Product product = findProductById(productId);
        if (product != null) {
            // Can call package-private method
            product.setPrice(newPrice);
            System.out.println("Price updated for " + productId + ": $" + newPrice);
        } else {
            System.out.println("Product not found: " + productId);
        }
    }
    
    public void adjustStock(String productId, int adjustment) {
        Product product = findProductById(productId);
        if (product != null) {
            // Can call package-private method
            product.updateStock(adjustment);
            System.out.println("Stock adjusted for " + productId + ": " + adjustment);
        } else {
            System.out.println("Product not found: " + productId);
        }
    }
    
    public Product findProductById(String productId) {
        return products.stream()
                      .filter(p -> p.getProductId().equals(productId))
                      .findFirst()
                      .orElse(null);
    }
    
    public List<Product> getAllProducts() {
        return new ArrayList<>(products); // Return copy for encapsulation
    }
    
    public List<Product> getProductsByCategory(String category) {
        return products.stream()
                      .filter(p -> p.getCategory().equalsIgnoreCase(category))
                      .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    public void displayInventory() {
        System.out.println("=== Product Inventory ===");
        if (products.isEmpty()) {
            System.out.println("No products in inventory");
        } else {
            products.forEach(System.out::println);
        }
        System.out.println();
    }
}
```

**File 3: `PackageDemo.java` (in default package)**

```java
// Import specific classes
import com.javalearning.model.Product;
import com.javalearning.service.ProductService;

// Import all classes from a package
// import com.javalearning.model.*;

public class PackageDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Package Organization Demo ===\n");
        
        // Create service
        ProductService productService = new ProductService();
        
        // Create products using service
        Product laptop = productService.createProduct("Dell Laptop", "Electronics", 999.99, 10);
        Product mouse = productService.createProduct("Wireless Mouse", "Electronics", 29.99, 50);
        Product book = productService.createProduct("Java Programming", "Books", 49.99, 25);
        
        // Display inventory
        productService.displayInventory();
        
        // Test service operations
        System.out.println("=== Testing Service Operations ===");
        productService.updateProductPrice("PROD1001", 899.99);
        productService.adjustStock("PROD1002", -5);
        productService.adjustStock("PROD1003", 10);
        
        // Display updated inventory
        productService.displayInventory();
        
        // Test public access to Product
        System.out.println("=== Testing Public Access ===");
        System.out.println("Laptop details:");
        System.out.println("ID: " + laptop.getProductId());
        System.out.println("Name: " + laptop.getName());
        System.out.println("Price: $" + laptop.getPrice());
        System.out.println("Stock: " + laptop.getStockQuantity());
        
        // These would cause compilation errors (package-private access):
        // laptop.setPrice(500.0);        // Not accessible from different package
        // laptop.updateStock(5);         // Not accessible from different package
        // new Product(...);              // Constructor not accessible from different package
        
        // Find products by category
        System.out.println("\n=== Products by Category ===");
        var electronics = productService.getProductsByCategory("Electronics");
        System.out.println("Electronics: " + electronics.size() + " items");
        electronics.forEach(System.out::println);
    }
}
```

---

## 🎯 Today's Major Challenge

### **Challenge: Complete Employee Management System**

Create `EmployeeManagementSystem.java`:

```java
import java.time.LocalDate;
import java.util.*;

/**
 * Complete Employee Management System demonstrating encapsulation principles
 */
public class EmployeeManagementSystem {
    
    // Private data - fully encapsulated
    private Map<String, Employee> employees;
    private Map<String, List<Employee>> departmentEmployees;
    private static final String[] VALID_DEPARTMENTS = {
        "Engineering", "Marketing", "Sales", "HR", "Finance", "Operations", "IT"
    };
    
    public EmployeeManagementSystem() {
        this.employees = new HashMap<>();
        this.departmentEmployees = new HashMap<>();
        
        // Initialize department lists
        for (String dept : VALID_DEPARTMENTS) {
            this.departmentEmployees.put(dept, new ArrayList<>());
        }
    }
    
    // Public interface methods
    public boolean addEmployee(String firstName, String lastName, String email, 
                              String department, String position, double salary, LocalDate birthDate) {
        try {
            Employee employee = new Employee(firstName, lastName, email, department, position, salary, birthDate);
            String employeeId = employee.getEmployeeId();
            
            // Add to main collection
            this.employees.put(employeeId, employee);
            
            // Add to department collection
            this.departmentEmployees.get(employee.getDepartment()).add(employee);
            
            System.out.println("Employee successfully added to system: " + employee.getFullName());
            return true;
            
        } catch (IllegalArgumentException e) {
            System.err.println("Failed to add employee: " + e.getMessage());
            return false;
        }
    }
    
    public Employee getEmployee(String employeeId) {
        return this.employees.get(employeeId);
    }
    
    public List<Employee> getAllEmployees() {
        return new ArrayList<>(this.employees.values());
    }
    
    public List<Employee> getEmployeesByDepartment(String department) {
        List<Employee> deptList = this.departmentEmployees.get(department);
        return deptList != null ? new ArrayList<>(deptList) : new ArrayList<>();
    }
    
    public boolean removeEmployee(String employeeId) {
        Employee employee = this.employees.remove(employeeId);
        if (employee != null) {
            employee.setActive(false);
            this.departmentEmployees.get(employee.getDepartment()).remove(employee);
            System.out.println("Employee removed: " + employee.getFullName());
            return true;
        } else {
            System.out.println("Employee not found: " + employeeId);
            return false;
        }
    }
    
    public void giveRaiseToEmployee(String employeeId, double percentage) {
        Employee employee = this.employees.get(employeeId);
        if (employee != null) {
            employee.giveRaise(percentage);
        } else {
            System.out.println("Employee not found: " + employeeId);
        }
    }
    
    public void giveRaiseToDepartment(String department, double percentage) {
        List<Employee> deptEmployees = this.departmentEmployees.get(department);
        if (deptEmployees != null) {
            System.out.println("Giving " + percentage + "% raise to all employees in " + department);
            for (Employee emp : deptEmployees) {
                emp.giveRaise(percentage);
            }
        } else {
            System.out.println("Invalid department: " + department);
        }
    }
    
    // Read-only access to statistics
    public int getTotalEmployeeCount() {
        return this.employees.size();
    }
    
    public int getDepartmentEmployeeCount(String department) {
        List<Employee> deptList = this.departmentEmployees.get(department);
        return deptList != null ? deptList.size() : 0;
    }
    
    public double getAverageSalary() {
        if (this.employees.isEmpty()) return 0.0;
        
        return this.employees.values().stream()
                           .mapToDouble(Employee::getSalary)
                           .average()
                           .orElse(0.0);
    }
    
    public double getDepartmentAverageSalary(String department) {
        List<Employee> deptEmployees = this.departmentEmployees.get(department);
        if (deptEmployees == null || deptEmployees.isEmpty()) return 0.0;
        
        return deptEmployees.stream()
                          .mapToDouble(Employee::getSalary)
                          .average()
                          .orElse(0.0);
    }
    
    public List<Employee> getTopEarners(int count) {
        return this.employees.values().stream()
                           .sorted((e1, e2) -> Double.compare(e2.getSalary(), e1.getSalary()))
                           .limit(count)
                           .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    // Display methods
    public void displayAllEmployees() {
        System.out.println("=== All Employees ===");
        if (this.employees.isEmpty()) {
            System.out.println("No employees in system");
        } else {
            this.employees.values().forEach(Employee::displayBasicInfo);
        }
        System.out.println();
    }
    
    public void displayDepartmentSummary() {
        System.out.println("=== Department Summary ===");
        for (String dept : VALID_DEPARTMENTS) {
            int count = getDepartmentEmployeeCount(dept);
            double avgSalary = getDepartmentAverageSalary(dept);
            System.out.printf("%-12s: %2d employees, Avg Salary: $%,.2f%n", 
                             dept, count, avgSalary);
        }
        System.out.println();
    }
    
    public void displaySystemStatistics() {
        System.out.println("=== System Statistics ===");
        System.out.println("Total Employees: " + getTotalEmployeeCount());
        System.out.printf("Average Salary: $%,.2f%n", getAverageSalary());
        
        System.out.println("\nTop 3 Earners:");
        List<Employee> topEarners = getTopEarners(3);
        for (int i = 0; i < topEarners.size(); i++) {
            Employee emp = topEarners.get(i);
            System.out.printf("%d. %s - %s%n", 
                             i + 1, emp.getFullName(), emp.getFormattedSalary());
        }
        System.out.println();
    }
    
    // Test the complete system
    public static void main(String[] args) {
        System.out.println("=== Employee Management System Demo ===\n");
        
        EmployeeManagementSystem ems = new EmployeeManagementSystem();
        
        // Add employees
        System.out.println("=== Adding Employees ===");
        ems.addEmployee("John", "Doe", "john.doe@company.com", 
                       "Engineering", "Software Developer", 85000, LocalDate.of(1990, 5, 15));
        
        ems.addEmployee("Jane", "Smith", "jane.smith@company.com", 
                       "Marketing", "Marketing Manager", 75000, LocalDate.of(1988, 8, 20));
        
        ems.addEmployee("Bob", "Johnson", "bob.johnson@company.com", 
                       "Engineering", "Senior Developer", 95000, LocalDate.of(1985, 3, 10));
        
        ems.addEmployee("Alice", "Brown", "alice.brown@company.com", 
                       "HR", "HR Specialist", 65000, LocalDate.of(1992, 11, 5));
        
        ems.addEmployee("Charlie", "Wilson", "charlie.wilson@company.com", 
                       "Sales", "Sales Representative", 60000, LocalDate.of(1995, 7, 25));
        
        // Try to add invalid employee
        ems.addEmployee("", "Invalid", "invalid-email", 
                       "InvalidDept", "Position", -1000, LocalDate.of(2010, 1, 1));
        
        System.out.println();
        
        // Display system state
        ems.displayAllEmployees();
        ems.displayDepartmentSummary();
        ems.displaySystemStatistics();
        
        // Test business operations
        System.out.println("=== Testing Business Operations ===");
        ems.giveRaiseToEmployee("EMP1001", 10.0);
        ems.giveRaiseToDepartment("Engineering", 5.0);
        
        // Final statistics
        System.out.println("\n=== Final System State ===");
        ems.displaySystemStatistics();
        ems.displayDepartmentSummary();
    }
}
```

---

## 📝 Key Takeaways from Day 6

### **Encapsulation Mastery:**
- ✅ **Access modifiers:** private, default, protected, public - when to use each
- ✅ **Getter/setter design:** Validation, computed properties, defensive copying
- ✅ **Data protection:** Preventing unauthorized access and modification
- ✅ **Business rules:** Enforcing constraints through encapsulation

### **Immutable Classes:**
- ✅ **Thread safety:** Immutable objects are inherently thread-safe
- ✅ **Defensive copying:** Protecting against external modification
- ✅ **Builder pattern alternative:** Using "with" methods for modifications
- ✅ **equals/hashCode:** Proper implementation for immutable objects

### **Package Organization:**
- ✅ **Package structure:** Logical organization of related classes
- ✅ **Import statements:** Controlling dependencies and namespace
- ✅ **Package-private access:** Controlled access within related classes
- ✅ **Separation of concerns:** Model vs service vs utility classes

### **Professional Practices:**
- ✅ **Input validation:** Comprehensive checking in setters
- ✅ **Error handling:** Meaningful exception messages
- ✅ **Documentation:** Clear javadoc and comments
- ✅ **Code organization:** Logical method grouping and class design

---

## 🚀 Tomorrow's Preview (Day 7 - July 16)

**Focus:** Inheritance - Code Reuse and IS-A Relationships
- extends keyword and class hierarchies
- Method overriding vs overloading
- super keyword for parent access
- Object class methods (toString, equals, hashCode)
- final classes and methods

---

## ✅ Day 6 Checklist

**Completed Tasks:**
- [ ] Master all access modifiers and their appropriate usage
- [ ] Create getters and setters with comprehensive validation
- [ ] Build immutable classes for thread-safe programming
- [ ] Organize code with packages and proper imports
- [ ] Implement defensive programming techniques
- [ ] Build complete Employee Management System
- [ ] Apply encapsulation best practices throughout
- [ ] Commit code to GitHub: "Day 6: Encapsulation and Data Hiding mastery"

**Bonus Achievements:**
- [ ] Add more validation rules to Employee class
- [ ] Create additional immutable classes
- [ ] Design a complete package structure for a project
- [ ] Implement builder pattern for complex object creation

---

## 🎉 Professional-Level Achievement!

Outstanding work! You've now mastered one of the most important concepts in object-oriented programming - **encapsulation**. You can now:

- **Protect data integrity** with proper access control
- **Build thread-safe applications** with immutable classes  
- **Organize large codebases** with package structures
- **Write maintainable code** that follows industry best practices

Tomorrow we'll explore inheritance - the powerful mechanism that lets you build upon existing classes and create rich object hierarchies!

**You're writing professional-quality Java code now!** 🚀