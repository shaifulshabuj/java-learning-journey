# Day 2: Variables, Data Types & Operators
**Date:** July 11, 2025  
**Duration:** 1.5 hours (Combined Session)  
**Focus:** Master Java's type system and basic operations

---

## 🎯 Today's Learning Goals
By the end of this session, you'll:
- ✅ Master all Java primitive data types
- ✅ Understand variable declaration and initialization
- ✅ Work with constants using `final` keyword
- ✅ Perform type casting and conversions
- ✅ Use all major operators (arithmetic, logical, comparison)
- ✅ Master String operations and methods

---

## 📚 Part 1: Variables & Data Types (30 minutes)

### **Java Primitive Data Types**

Java has 8 primitive data types, each with specific size and purpose:

| Type | Size | Range | Example |
|------|------|--------|---------|
| `byte` | 1 byte | -128 to 127 | `byte age = 25;` |
| `short` | 2 bytes | -32,768 to 32,767 | `short year = 2025;` |
| `int` | 4 bytes | -2³¹ to 2³¹-1 | `int salary = 50000;` |
| `long` | 8 bytes | -2⁶³ to 2⁶³-1 | `long population = 8000000000L;` |
| `float` | 4 bytes | ~7 decimal digits | `float pi = 3.14f;` |
| `double` | 8 bytes | ~15 decimal digits | `double precise = 3.14159265359;` |
| `char` | 2 bytes | Unicode characters | `char grade = 'A';` |
| `boolean` | 1 bit | true or false | `boolean isActive = true;` |

### **Variable Declaration Patterns**

```java
// Declaration only
int number;

// Declaration with initialization
int count = 10;

// Multiple variables of same type
int x = 5, y = 10, z = 15;

// Constants (cannot be changed)
final int MAX_SIZE = 100;
final double PI = 3.14159;
```

### **Naming Conventions**

```java
// Good variable names (camelCase)
int studentAge;
String firstName;
boolean isCompleted;
double accountBalance;

// Constants (UPPER_SNAKE_CASE)
final int MAX_ATTEMPTS = 3;
final String DATABASE_URL = "localhost:3306";
```

---

## 💻 Part 2: Hands-On Practice - Data Types (20 minutes)

### **Exercise 1: Data Types Showcase**

Create `DataTypesDemo.java`:

```java
public class DataTypesDemo {
    public static void main(String[] args) {
        System.out.println("=== Java Data Types Demo ===\n");
        
        // Integer types
        byte smallNumber = 127;
        short mediumNumber = 32000;
        int regularNumber = 2000000;
        long bigNumber = 9000000000L; // Note the 'L' suffix
        
        System.out.println("Integer Types:");
        System.out.println("byte: " + smallNumber);
        System.out.println("short: " + mediumNumber);
        System.out.println("int: " + regularNumber);
        System.out.println("long: " + bigNumber);
        
        // Floating point types
        float price = 199.99f; // Note the 'f' suffix
        double preciseValue = 3.141592653589793;
        
        System.out.println("\nFloating Point Types:");
        System.out.println("float: " + price);
        System.out.println("double: " + preciseValue);
        
        // Character and Boolean
        char letterGrade = 'A';
        boolean isPassingGrade = true;
        
        System.out.println("\nOther Types:");
        System.out.println("char: " + letterGrade);
        System.out.println("boolean: " + isPassingGrade);
        
        // Constants
        final int DAYS_IN_WEEK = 7;
        final String COMPANY_NAME = "TechCorp";
        
        System.out.println("\nConstants:");
        System.out.println("Days in week: " + DAYS_IN_WEEK);
        System.out.println("Company: " + COMPANY_NAME);
        
        // Size demonstration
        System.out.println("\nType Sizes (in bytes):");
        System.out.println("byte: " + Byte.BYTES);
        System.out.println("short: " + Short.BYTES);
        System.out.println("int: " + Integer.BYTES);
        System.out.println("long: " + Long.BYTES);
        System.out.println("float: " + Float.BYTES);
        System.out.println("double: " + Double.BYTES);
        System.out.println("char: " + Character.BYTES);
    }
}
```

---

## 🔄 Part 3: Type Casting & Conversion (15 minutes)

### **Automatic Type Promotion (Widening)**
```java
public class TypeCasting {
    public static void main(String[] args) {
        System.out.println("=== Type Casting Demo ===\n");
        
        // Automatic casting (smaller to larger)
        byte byteValue = 100;
        int intValue = byteValue; // Automatic casting
        long longValue = intValue; // Automatic casting
        double doubleValue = longValue; // Automatic casting
        
        System.out.println("Automatic Casting:");
        System.out.println("byte to int: " + byteValue + " → " + intValue);
        System.out.println("int to long: " + intValue + " → " + longValue);
        System.out.println("long to double: " + longValue + " → " + doubleValue);
        
        // Manual casting (larger to smaller) - BE CAREFUL!
        double largeNumber = 123.456;
        int truncatedNumber = (int) largeNumber; // Explicit casting
        
        System.out.println("\nExplicit Casting:");
        System.out.println("double to int: " + largeNumber + " → " + truncatedNumber);
        
        // Character casting
        char letter = 'A';
        int asciiValue = letter; // char to int
        char backToChar = (char) (asciiValue + 1);
        
        System.out.println("\nCharacter Casting:");
        System.out.println("char '" + letter + "' has ASCII value: " + asciiValue);
        System.out.println("Next character: " + backToChar);
        
        // String to number conversion
        String numberString = "12345";
        int convertedNumber = Integer.parseInt(numberString);
        
        System.out.println("\nString Conversion:");
        System.out.println("String \"" + numberString + "\" to int: " + convertedNumber);
    }
}
```

---

## ➕ Part 4: Operators (25 minutes)

### **All Java Operators**

Create `OperatorsDemo.java`:

```java
public class OperatorsDemo {
    public static void main(String[] args) {
        System.out.println("=== Java Operators Demo ===\n");
        
        int a = 15, b = 4;
        
        // Arithmetic Operators
        System.out.println("Arithmetic Operators (a=15, b=4):");
        System.out.println("a + b = " + (a + b)); // Addition
        System.out.println("a - b = " + (a - b)); // Subtraction
        System.out.println("a * b = " + (a * b)); // Multiplication
        System.out.println("a / b = " + (a / b)); // Division (integer)
        System.out.println("a % b = " + (a % b)); // Modulus (remainder)
        
        // Division with doubles for decimal result
        double preciseDiv = (double) a / b;
        System.out.println("a / b (precise) = " + preciseDiv);
        
        // Assignment Operators
        System.out.println("\nAssignment Operators:");
        int x = 10;
        System.out.println("x = " + x);
        x += 5; // x = x + 5
        System.out.println("x += 5: " + x);
        x -= 3; // x = x - 3
        System.out.println("x -= 3: " + x);
        x *= 2; // x = x * 2
        System.out.println("x *= 2: " + x);
        x /= 4; // x = x / 4
        System.out.println("x /= 4: " + x);
        
        // Increment/Decrement Operators
        System.out.println("\nIncrement/Decrement Operators:");
        int count = 5;
        System.out.println("Original count: " + count);
        System.out.println("count++: " + count++); // Post-increment
        System.out.println("After post-increment: " + count);
        System.out.println("++count: " + ++count); // Pre-increment
        System.out.println("count--: " + count--); // Post-decrement
        System.out.println("--count: " + --count); // Pre-decrement
        
        // Comparison Operators
        System.out.println("\nComparison Operators (a=15, b=4):");
        System.out.println("a == b: " + (a == b)); // Equal to
        System.out.println("a != b: " + (a != b)); // Not equal to
        System.out.println("a > b: " + (a > b));   // Greater than
        System.out.println("a < b: " + (a < b));   // Less than
        System.out.println("a >= b: " + (a >= b)); // Greater than or equal
        System.out.println("a <= b: " + (a <= b)); // Less than or equal
        
        // Logical Operators
        System.out.println("\nLogical Operators:");
        boolean condition1 = true;
        boolean condition2 = false;
        
        System.out.println("condition1 && condition2: " + (condition1 && condition2)); // AND
        System.out.println("condition1 || condition2: " + (condition1 || condition2)); // OR
        System.out.println("!condition1: " + (!condition1)); // NOT
        
        // Practical example
        int age = 20;
        boolean hasLicense = true;
        boolean canDrive = age >= 18 && hasLicense;
        System.out.println("Can drive (age>=18 AND has license): " + canDrive);
    }
}
```

---

## 🔤 Part 5: String Operations (20 minutes)

### **String Methods & Operations**

Create `StringOperations.java`:

```java
public class StringOperations {
    public static void main(String[] args) {
        System.out.println("=== String Operations Demo ===\n");
        
        // String creation
        String greeting = "Hello";
        String name = "Java Developer";
        String message = greeting + " " + name + "!";
        
        System.out.println("String Creation:");
        System.out.println("greeting: " + greeting);
        System.out.println("name: " + name);
        System.out.println("message: " + message);
        
        // String properties
        System.out.println("\nString Properties:");
        System.out.println("Length of message: " + message.length());
        System.out.println("Is message empty? " + message.isEmpty());
        
        // String methods
        System.out.println("\nString Methods:");
        System.out.println("Uppercase: " + message.toUpperCase());
        System.out.println("Lowercase: " + message.toLowerCase());
        System.out.println("First character: " + message.charAt(0));
        System.out.println("Last character: " + message.charAt(message.length() - 1));
        
        // String searching
        System.out.println("\nString Searching:");
        System.out.println("Contains 'Java': " + message.contains("Java"));
        System.out.println("Starts with 'Hello': " + message.startsWith("Hello"));
        System.out.println("Ends with '!': " + message.endsWith("!"));
        System.out.println("Index of 'Java': " + message.indexOf("Java"));
        
        // String manipulation
        System.out.println("\nString Manipulation:");
        String original = "  Java Programming  ";
        System.out.println("Original: '" + original + "'");
        System.out.println("Trimmed: '" + original.trim() + "'");
        System.out.println("Replaced: " + original.replace("Java", "Python"));
        
        // String splitting
        String data = "apple,banana,orange,grape";
        String[] fruits = data.split(",");
        System.out.println("\nString Splitting:");
        System.out.println("Original: " + data);
        System.out.println("Split into array:");
        for (int i = 0; i < fruits.length; i++) {
            System.out.println("  [" + i + "]: " + fruits[i]);
        }
        
        // String comparison
        System.out.println("\nString Comparison:");
        String str1 = "Hello";
        String str2 = "hello";
        String str3 = "Hello";
        
        System.out.println("str1.equals(str2): " + str1.equals(str2));
        System.out.println("str1.equals(str3): " + str1.equals(str3));
        System.out.println("str1.equalsIgnoreCase(str2): " + str1.equalsIgnoreCase(str2));
        
        // String formatting
        System.out.println("\nString Formatting:");
        String template = "My name is %s, I am %d years old, and I earn $%.2f";
        String formatted = String.format(template, "John", 25, 50000.50);
        System.out.println(formatted);
    }
}
```

---

## 🎯 Today's Challenges

### **Challenge 1: Personal Calculator**

Create `PersonalCalculator.java`:

```java
public class PersonalCalculator {
    public static void main(String[] args) {
        System.out.println("=== Personal Financial Calculator ===\n");
        
        // Your financial data
        final double MONTHLY_SALARY = 75000.0; // Your monthly salary
        final int WORKING_DAYS_PER_MONTH = 22;
        final double TAX_RATE = 0.15; // 15% tax
        
        // Calculate daily and hourly rates
        double dailySalary = MONTHLY_SALARY / WORKING_DAYS_PER_MONTH;
        double hourlySalary = dailySalary / 8; // 8 hours per day
        
        // Calculate taxes and net salary
        double monthlyTax = MONTHLY_SALARY * TAX_RATE;
        double netMonthlySalary = MONTHLY_SALARY - monthlyTax;
        double annualSalary = MONTHLY_SALARY * 12;
        double netAnnualSalary = netMonthlySalary * 12;
        
        // Display results
        System.out.println("Salary Breakdown:");
        System.out.printf("Monthly Gross Salary: $%.2f%n", MONTHLY_SALARY);
        System.out.printf("Daily Salary: $%.2f%n", dailySalary);
        System.out.printf("Hourly Salary: $%.2f%n", hourlySalary);
        System.out.println();
        
        System.out.println("Tax Calculations:");
        System.out.printf("Tax Rate: %.1f%%%n", TAX_RATE * 100);
        System.out.printf("Monthly Tax: $%.2f%n", monthlyTax);
        System.out.printf("Net Monthly Salary: $%.2f%n", netMonthlySalary);
        System.out.println();
        
        System.out.println("Annual Summary:");
        System.out.printf("Annual Gross Salary: $%.2f%n", annualSalary);
        System.out.printf("Annual Net Salary: $%.2f%n", netAnnualSalary);
        
        // Bonus: Calculate savings potential
        double monthlyExpenses = 45000.0; // Estimate your monthly expenses
        double monthlySavings = netMonthlySalary - monthlyExpenses;
        double annualSavings = monthlySavings * 12;
        
        System.out.println("\nSavings Potential:");
        System.out.printf("Monthly Expenses: $%.2f%n", monthlyExpenses);
        System.out.printf("Monthly Savings: $%.2f%n", monthlySavings);
        System.out.printf("Annual Savings: $%.2f%n", annualSavings);
        
        if (monthlySavings > 0) {
            System.out.println("Great! You're saving money each month! 💰");
        } else {
            System.out.println("Consider reducing expenses to save money. 📊");
        }
    }
}
```

### **Challenge 2: Data Type Converter**

Create `DataTypeConverter.java`:

```java
public class DataTypeConverter {
    public static void main(String[] args) {
        System.out.println("=== Data Type Converter Utility ===\n");
        
        // Temperature conversions
        double celsius = 25.0;
        double fahrenheit = (celsius * 9.0 / 5.0) + 32.0;
        double kelvin = celsius + 273.15;
        
        System.out.println("Temperature Conversions:");
        System.out.printf("%.1f°C = %.1f°F = %.1fK%n", celsius, fahrenheit, kelvin);
        
        // Distance conversions
        double kilometers = 10.5;
        double miles = kilometers * 0.621371;
        double meters = kilometers * 1000;
        double feet = meters * 3.28084;
        
        System.out.println("\nDistance Conversions:");
        System.out.printf("%.1f km = %.2f miles = %.0f meters = %.0f feet%n", 
                         kilometers, miles, meters, feet);
        
        // Weight conversions
        double kilograms = 70.5;
        double pounds = kilograms * 2.20462;
        double grams = kilograms * 1000;
        double ounces = pounds * 16;
        
        System.out.println("\nWeight Conversions:");
        System.out.printf("%.1f kg = %.1f lbs = %.0f g = %.0f oz%n", 
                         kilograms, pounds, grams, ounces);
        
        // Time conversions
        int totalSeconds = 7265; // Example: 2 hours, 1 minute, 5 seconds
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        
        System.out.println("\nTime Conversion:");
        System.out.printf("%d seconds = %d hours, %d minutes, %d seconds%n", 
                         totalSeconds, hours, minutes, seconds);
        
        // Number base conversions
        int decimalNumber = 255;
        String binaryString = Integer.toBinaryString(decimalNumber);
        String hexString = Integer.toHexString(decimalNumber).toUpperCase();
        String octalString = Integer.toOctalString(decimalNumber);
        
        System.out.println("\nNumber Base Conversions:");
        System.out.printf("Decimal %d = Binary %s = Hex %s = Octal %s%n", 
                         decimalNumber, binaryString, hexString, octalString);
        
        // Character conversions
        char letter = 'A';
        int asciiValue = (int) letter;
        char nextLetter = (char) (asciiValue + 1);
        
        System.out.println("\nCharacter Conversions:");
        System.out.printf("Character '%c' has ASCII value %d%n", letter, asciiValue);
        System.out.printf("Next character is '%c' with ASCII value %d%n", 
                         nextLetter, (int) nextLetter);
    }
}
```

---

## 📝 Key Takeaways from Day 2

### **Data Types Mastered:**
- ✅ **Primitive Types:** byte, short, int, long, float, double, char, boolean
- ✅ **Type Sizes:** Understanding memory usage
- ✅ **Constants:** Using `final` keyword
- ✅ **Naming Conventions:** camelCase and UPPER_SNAKE_CASE

### **Type Casting:**
- ✅ **Automatic Casting:** Smaller to larger types
- ✅ **Explicit Casting:** Larger to smaller types (with data loss risk)
- ✅ **String Conversions:** parseInt(), parseDouble(), etc.

### **Operators:**
- ✅ **Arithmetic:** +, -, *, /, %
- ✅ **Assignment:** =, +=, -=, *=, /=
- ✅ **Increment/Decrement:** ++, --
- ✅ **Comparison:** ==, !=, >, <, >=, <=
- ✅ **Logical:** &&, ||, !

### **String Operations:**
- ✅ **Basic Methods:** length(), charAt(), toUpperCase(), toLowerCase()
- ✅ **Search Methods:** contains(), indexOf(), startsWith(), endsWith()
- ✅ **Manipulation:** trim(), replace(), split()
- ✅ **Comparison:** equals(), equalsIgnoreCase()
- ✅ **Formatting:** String.format(), printf()

---

## 🚀 Tomorrow's Preview (Day 3 - July 12)

**Focus:** Control Flow Structures
- if-else statements and nested conditions
- switch statements (traditional and enhanced)
- for loops, while loops, do-while loops
- break and continue statements
- Practice with decision-making programs

---

## ✅ Day 2 Checklist

**Completed Tasks:**
- [ ] Understand all 8 primitive data types
- [ ] Practice variable declaration and initialization
- [ ] Master type casting and conversions
- [ ] Use all major operators effectively
- [ ] Work with String methods and operations
- [ ] Complete Personal Calculator challenge
- [ ] Complete Data Type Converter challenge
- [ ] Commit code to GitHub with message: "Day 2: Variables, Data Types, and Operators mastery"

**Bonus Achievements:**
- [ ] Experiment with different number formats
- [ ] Try edge cases with type casting
- [ ] Create additional utility programs
- [ ] Research Java documentation for more String methods

---

## 🎉 Excellent Progress!

You're building a solid foundation in Java! Tomorrow we'll add decision-making and looping capabilities to your programs, making them much more interactive and powerful.

**Keep up the fantastic momentum!** 🚀