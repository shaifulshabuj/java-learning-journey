# Day 4: Methods & Scope - Building Reusable Code
**Date:** July 13, 2025  
**Duration:** 1.5 hours (Combined Session)  
**Focus:** Method creation, parameters, return types, scope, and recursion

---

## 🎯 Today's Learning Goals
By the end of this session, you'll:
- ✅ Create methods with different parameter types and return values
- ✅ Master method overloading for flexible functionality
- ✅ Understand variable scope (local, instance, class/static)
- ✅ Use static vs non-static methods effectively
- ✅ Implement recursive algorithms
- ✅ Build a comprehensive utility library
- ✅ Organize code for maximum reusability

---

## 📚 Part 1: Method Fundamentals (20 minutes)

### **Method Anatomy**

```java
[access_modifier] [static] return_type methodName(parameter_list) {
    // Method body
    return value; // if return_type is not void
}
```

### **Basic Method Examples**

Create `MethodBasics.java`:

```java
public class MethodBasics {
    
    // Method with no parameters, no return value
    public static void greetUser() {
        System.out.println("Hello! Welcome to Java Methods!");
        System.out.println("Today is a great day to learn programming!");
    }
    
    // Method with parameters, no return value
    public static void greetUserByName(String name, int age) {
        System.out.println("Hello " + name + "!");
        System.out.println("You are " + age + " years old.");
        
        if (age >= 18) {
            System.out.println("You are an adult!");
        } else {
            System.out.println("You are a minor.");
        }
    }
    
    // Method with parameters and return value
    public static int addTwoNumbers(int num1, int num2) {
        int sum = num1 + num2;
        return sum; // Can also write: return num1 + num2;
    }
    
    // Method with different parameter types
    public static double calculateRectangleArea(double length, double width) {
        return length * width;
    }
    
    // Method returning boolean
    public static boolean isEven(int number) {
        return number % 2 == 0;
    }
    
    // Method with multiple return statements
    public static String getGradeDescription(char grade) {
        switch (grade) {
            case 'A':
                return "Excellent performance!";
            case 'B':
                return "Good work!";
            case 'C':
                return "Satisfactory";
            case 'D':
                return "Needs improvement";
            case 'F':
                return "Failing grade";
            default:
                return "Invalid grade";
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Method Basics Demo ===\n");
        
        // Calling methods
        greetUser();
        System.out.println();
        
        greetUserByName("Alice", 25);
        greetUserByName("Bob", 16);
        System.out.println();
        
        // Using return values
        int result = addTwoNumbers(15, 25);
        System.out.println("15 + 25 = " + result);
        
        double area = calculateRectangleArea(5.5, 3.2);
        System.out.printf("Rectangle area: %.2f%n", area);
        
        // Using boolean return
        int testNumber = 42;
        if (isEven(testNumber)) {
            System.out.println(testNumber + " is even");
        } else {
            System.out.println(testNumber + " is odd");
        }
        
        // Using string return
        System.out.println("Grade A: " + getGradeDescription('A'));
        System.out.println("Grade F: " + getGradeDescription('F'));
    }
}
```

---

## 🔄 Part 2: Method Overloading (15 minutes)

### **Multiple Methods with Same Name**

Create `MethodOverloading.java`:

```java
public class MethodOverloading {
    
    // Overloaded calculate methods - different parameter types/counts
    
    // Calculate area of square
    public static double calculateArea(double side) {
        System.out.println("Calculating square area...");
        return side * side;
    }
    
    // Calculate area of rectangle
    public static double calculateArea(double length, double width) {
        System.out.println("Calculating rectangle area...");
        return length * width;
    }
    
    // Calculate area of circle
    public static double calculateArea(double radius, boolean isCircle) {
        System.out.println("Calculating circle area...");
        return Math.PI * radius * radius;
    }
    
    // Overloaded print methods
    public static void printInfo(String message) {
        System.out.println("Message: " + message);
    }
    
    public static void printInfo(int number) {
        System.out.println("Number: " + number);
    }
    
    public static void printInfo(double decimal) {
        System.out.printf("Decimal: %.2f%n", decimal);
    }
    
    public static void printInfo(String name, int age) {
        System.out.println("Name: " + name + ", Age: " + age);
    }
    
    // Overloaded math operations
    public static int add(int a, int b) {
        return a + b;
    }
    
    public static double add(double a, double b) {
        return a + b;
    }
    
    public static int add(int a, int b, int c) {
        return a + b + c;
    }
    
    public static double add(double a, double b, double c) {
        return a + b + c;
    }
    
    // Variable arguments (varargs) - advanced overloading
    public static int sum(int... numbers) {
        int total = 0;
        for (int num : numbers) {
            total += num;
        }
        return total;
    }
    
    public static void main(String[] args) {
        System.out.println("=== Method Overloading Demo ===\n");
        
        // Area calculations - same method name, different parameters
        System.out.println("Square (5x5): " + calculateArea(5.0));
        System.out.println("Rectangle (4x6): " + calculateArea(4.0, 6.0));
        System.out.println("Circle (radius 3): " + calculateArea(3.0, true));
        System.out.println();
        
        // Print overloading
        printInfo("Hello World");
        printInfo(42);
        printInfo(3.14159);
        printInfo("John", 25);
        System.out.println();
        
        // Math overloading
        System.out.println("Add integers: " + add(5, 3));
        System.out.println("Add doubles: " + add(2.5, 3.7));
        System.out.println("Add three integers: " + add(1, 2, 3));
        System.out.println("Add three doubles: " + add(1.1, 2.2, 3.3));
        System.out.println();
        
        // Variable arguments
        System.out.println("Sum of 1,2,3: " + sum(1, 2, 3));
        System.out.println("Sum of 1,2,3,4,5: " + sum(1, 2, 3, 4, 5));
        System.out.println("Sum of 10,20: " + sum(10, 20));
    }
}
```

---

## 🎯 Part 3: Variable Scope (20 minutes)

### **Understanding Different Scopes**

Create `VariableScope.java`:

```java
public class VariableScope {
    
    // Class variables (static) - belong to the class
    static int classCounter = 0;
    static String className = "VariableScope";
    
    // Instance variables - belong to object instances
    String instanceName;
    int instanceId;
    
    // Constructor for instance variables
    public VariableScope(String name, int id) {
        this.instanceName = name;
        this.instanceId = id;
        classCounter++; // Increment class counter
    }
    
    // Static method - can only access static variables directly
    public static void displayClassInfo() {
        System.out.println("Class: " + className);
        System.out.println("Total instances created: " + classCounter);
        // System.out.println(instanceName); // ERROR! Can't access instance variables
    }
    
    // Instance method - can access both static and instance variables
    public void displayInstanceInfo() {
        System.out.println("Instance Name: " + instanceName);
        System.out.println("Instance ID: " + instanceId);
        System.out.println("Class Name: " + className); // Can access static variables
    }
    
    // Method demonstrating local scope
    public static void demonstrateLocalScope() {
        System.out.println("\n=== Local Scope Demo ===");
        
        // Local variables - only exist within this method
        int localVariable = 100;
        String localMessage = "This is local";
        
        System.out.println("Local variable: " + localVariable);
        System.out.println("Local message: " + localMessage);
        
        // Block scope within method
        if (true) {
            int blockVariable = 200; // Only exists in this block
            System.out.println("Block variable: " + blockVariable);
            System.out.println("Can access local: " + localVariable);
        }
        
        // System.out.println(blockVariable); // ERROR! Out of scope
        
        // Loop scope
        for (int i = 0; i < 3; i++) {
            int loopVariable = i * 10;
            System.out.println("Loop iteration " + i + ", loop variable: " + loopVariable);
        }
        
        // System.out.println(i); // ERROR! Loop variable out of scope
        // System.out.println(loopVariable); // ERROR! Out of scope
    }
    
    // Method with parameters (parameters are local to the method)
    public static void methodWithParameters(int param1, String param2) {
        System.out.println("\n=== Parameter Scope ===");
        System.out.println("Parameter 1: " + param1);
        System.out.println("Parameter 2: " + param2);
        
        // Parameters are local variables
        param1 = 999; // This doesn't affect the original value passed in
        param2 = "Modified";
        
        System.out.println("Modified param1: " + param1);
        System.out.println("Modified param2: " + param2);
    }
    
    // Method showing variable shadowing
    public static void demonstrateShadowing() {
        System.out.println("\n=== Variable Shadowing ===");
        
        int number = 10; // Local variable
        
        System.out.println("Outer number: " + number);
        
        {
            int number2 = 20; // Different variable, no shadowing
            System.out.println("Block number2: " + number2);
            System.out.println("Can still access outer number: " + number);
        }
        
        // In Java, you cannot shadow local variables in the same method
        // This would cause a compilation error:
        // {
        //     int number = 30; // ERROR! Variable number is already defined
        // }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Variable Scope Demo ===\n");
        
        // Static variables can be accessed without creating objects
        System.out.println("Initial class counter: " + classCounter);
        
        // Create instances to demonstrate instance variables
        VariableScope obj1 = new VariableScope("Object1", 101);
        VariableScope obj2 = new VariableScope("Object2", 102);
        
        // Display class information
        displayClassInfo();
        System.out.println();
        
        // Display instance information
        obj1.displayInstanceInfo();
        System.out.println();
        obj2.displayInstanceInfo();
        
        // Demonstrate local scope
        demonstrateLocalScope();
        
        // Demonstrate parameter scope
        int originalValue = 42;
        String originalString = "Original";
        
        System.out.println("Before method call - Value: " + originalValue + ", String: " + originalString);
        methodWithParameters(originalValue, originalString);
        System.out.println("After method call - Value: " + originalValue + ", String: " + originalString);
        
        // Demonstrate shadowing
        demonstrateShadowing();
        
        // Final class counter
        System.out.println("\nFinal class counter: " + classCounter);
    }
}
```

---

## 🔄 Part 4: Recursion - Methods Calling Themselves (25 minutes)

### **Recursive Algorithms**

Create `RecursionDemo.java`:

```java
public class RecursionDemo {
    
    // Factorial using recursion
    public static long factorial(int n) {
        // Base case - stop the recursion
        if (n <= 1) {
            return 1;
        }
        // Recursive case - method calls itself
        return n * factorial(n - 1);
    }
    
    // Fibonacci sequence using recursion
    public static int fibonacci(int n) {
        // Base cases
        if (n <= 1) {
            return n;
        }
        // Recursive case
        return fibonacci(n - 1) + fibonacci(n - 2);
    }
    
    // Power calculation using recursion
    public static double power(double base, int exponent) {
        // Base case
        if (exponent == 0) {
            return 1;
        }
        // Handle negative exponents
        if (exponent < 0) {
            return 1 / power(base, -exponent);
        }
        // Recursive case
        return base * power(base, exponent - 1);
    }
    
    // Sum of digits using recursion
    public static int sumOfDigits(int number) {
        // Make number positive for calculation
        number = Math.abs(number);
        
        // Base case
        if (number < 10) {
            return number;
        }
        
        // Recursive case: last digit + sum of remaining digits
        return (number % 10) + sumOfDigits(number / 10);
    }
    
    // Reverse a string using recursion
    public static String reverseString(String str) {
        // Base case
        if (str.length() <= 1) {
            return str;
        }
        
        // Recursive case: last character + reverse of remaining string
        return str.charAt(str.length() - 1) + reverseString(str.substring(0, str.length() - 1));
    }
    
    // Check if string is palindrome using recursion
    public static boolean isPalindrome(String str) {
        // Clean the string (remove spaces, convert to lowercase)
        str = str.replaceAll("\\s+", "").toLowerCase();
        
        return isPalindromeHelper(str, 0, str.length() - 1);
    }
    
    private static boolean isPalindromeHelper(String str, int start, int end) {
        // Base case: if we've checked all characters
        if (start >= end) {
            return true;
        }
        
        // If characters don't match
        if (str.charAt(start) != str.charAt(end)) {
            return false;
        }
        
        // Recursive case: check inner characters
        return isPalindromeHelper(str, start + 1, end - 1);
    }
    
    // Binary search using recursion
    public static int binarySearch(int[] array, int target, int start, int end) {
        // Base case: element not found
        if (start > end) {
            return -1;
        }
        
        int mid = start + (end - start) / 2;
        
        // Base case: element found
        if (array[mid] == target) {
            return mid;
        }
        
        // Recursive cases
        if (target < array[mid]) {
            return binarySearch(array, target, start, mid - 1);
        } else {
            return binarySearch(array, target, mid + 1, end);
        }
    }
    
    // Tower of Hanoi - classic recursion problem
    public static void towerOfHanoi(int n, char fromRod, char toRod, char auxRod) {
        if (n == 1) {
            System.out.println("Move disk 1 from " + fromRod + " to " + toRod);
            return;
        }
        
        // Move n-1 disks from source to auxiliary rod
        towerOfHanoi(n - 1, fromRod, auxRod, toRod);
        
        // Move the largest disk from source to destination
        System.out.println("Move disk " + n + " from " + fromRod + " to " + toRod);
        
        // Move n-1 disks from auxiliary to destination rod
        towerOfHanoi(n - 1, auxRod, toRod, fromRod);
    }
    
    public static void main(String[] args) {
        System.out.println("=== Recursion Demo ===\n");
        
        // Factorial examples
        System.out.println("Factorial Examples:");
        for (int i = 0; i <= 10; i++) {
            System.out.println(i + "! = " + factorial(i));
        }
        System.out.println();
        
        // Fibonacci examples
        System.out.println("Fibonacci Sequence (first 15 numbers):");
        for (int i = 0; i < 15; i++) {
            System.out.print(fibonacci(i) + " ");
        }
        System.out.println("\n");
        
        // Power examples
        System.out.println("Power Examples:");
        System.out.println("2^5 = " + power(2, 5));
        System.out.println("3^4 = " + power(3, 4));
        System.out.println("5^0 = " + power(5, 0));
        System.out.println("2^(-3) = " + power(2, -3));
        System.out.println();
        
        // Sum of digits
        System.out.println("Sum of Digits:");
        int[] testNumbers = {123, 456, 789, 1234, 9876};
        for (int num : testNumbers) {
            System.out.println("Sum of digits in " + num + " = " + sumOfDigits(num));
        }
        System.out.println();
        
        // String reversal
        System.out.println("String Reversal:");
        String[] testStrings = {"hello", "Java", "recursion", "programming"};
        for (String str : testStrings) {
            System.out.println("\"" + str + "\" reversed = \"" + reverseString(str) + "\"");
        }
        System.out.println();
        
        // Palindrome check
        System.out.println("Palindrome Check:");
        String[] palindromeTests = {"racecar", "hello", "madam", "A man a plan a canal Panama", "programming"};
        for (String str : palindromeTests) {
            System.out.println("\"" + str + "\" is palindrome: " + isPalindrome(str));
        }
        System.out.println();
        
        // Binary search
        System.out.println("Binary Search:");
        int[] sortedArray = {2, 5, 8, 12, 16, 23, 38, 45, 56, 67, 78};
        int[] searchTargets = {23, 45, 99, 2, 78};
        
        System.out.print("Array: ");
        for (int num : sortedArray) {
            System.out.print(num + " ");
        }
        System.out.println();
        
        for (int target : searchTargets) {
            int index = binarySearch(sortedArray, target, 0, sortedArray.length - 1);
            if (index != -1) {
                System.out.println("Found " + target + " at index " + index);
            } else {
                System.out.println(target + " not found in array");
            }
        }
        System.out.println();
        
        // Tower of Hanoi
        System.out.println("Tower of Hanoi (3 disks):");
        towerOfHanoi(3, 'A', 'C', 'B');
    }
}
```

---

## 🎯 Today's Major Challenges

### **Challenge 1: Math Utility Library**

Create `MathUtilities.java`:

```java
public class MathUtilities {
    
    // Constants
    public static final double PI = 3.14159265359;
    public static final double E = 2.71828182846;
    
    // Basic arithmetic operations
    public static int add(int a, int b) {
        return a + b;
    }
    
    public static double add(double a, double b) {
        return a + b;
    }
    
    public static int subtract(int a, int b) {
        return a - b;
    }
    
    public static double subtract(double a, double b) {
        return a - b;
    }
    
    public static int multiply(int a, int b) {
        return a * b;
    }
    
    public static double multiply(double a, double b) {
        return a * b;
    }
    
    public static double divide(double a, double b) {
        if (b == 0) {
            System.out.println("Error: Division by zero!");
            return Double.NaN;
        }
        return a / b;
    }
    
    // Advanced mathematical functions
    public static double squareRoot(double number) {
        if (number < 0) {
            System.out.println("Error: Cannot calculate square root of negative number!");
            return Double.NaN;
        }
        return Math.sqrt(number);
    }
    
    public static double power(double base, double exponent) {
        return Math.pow(base, exponent);
    }
    
    public static long factorial(int n) {
        if (n < 0) {
            System.out.println("Error: Factorial not defined for negative numbers!");
            return -1;
        }
        if (n > 20) {
            System.out.println("Error: Factorial too large (overflow risk)!");
            return -1;
        }
        
        if (n <= 1) return 1;
        return n * factorial(n - 1);
    }
    
    // Number theory functions
    public static boolean isPrime(int number) {
        if (number <= 1) return false;
        if (number <= 3) return true;
        if (number % 2 == 0 || number % 3 == 0) return false;
        
        for (int i = 5; i * i <= number; i += 6) {
            if (number % i == 0 || number % (i + 2) == 0) {
                return false;
            }
        }
        return true;
    }
    
    public static int gcd(int a, int b) {
        a = Math.abs(a);
        b = Math.abs(b);
        
        if (b == 0) return a;
        return gcd(b, a % b);
    }
    
    public static int lcm(int a, int b) {
        return Math.abs(a * b) / gcd(a, b);
    }
    
    // Statistics functions
    public static double average(double... numbers) {
        if (numbers.length == 0) return 0;
        
        double sum = 0;
        for (double num : numbers) {
            sum += num;
        }
        return sum / numbers.length;
    }
    
    public static double max(double... numbers) {
        if (numbers.length == 0) return Double.NaN;
        
        double maximum = numbers[0];
        for (double num : numbers) {
            if (num > maximum) {
                maximum = num;
            }
        }
        return maximum;
    }
    
    public static double min(double... numbers) {
        if (numbers.length == 0) return Double.NaN;
        
        double minimum = numbers[0];
        for (double num : numbers) {
            if (num < minimum) {
                minimum = num;
            }
        }
        return minimum;
    }
    
    // Geometry functions
    public static double circleArea(double radius) {
        return PI * radius * radius;
    }
    
    public static double circleCircumference(double radius) {
        return 2 * PI * radius;
    }
    
    public static double rectangleArea(double length, double width) {
        return length * width;
    }
    
    public static double rectanglePerimeter(double length, double width) {
        return 2 * (length + width);
    }
    
    public static double triangleArea(double base, double height) {
        return 0.5 * base * height;
    }
    
    // Conversion functions
    public static double celsiusToFahrenheit(double celsius) {
        return (celsius * 9.0 / 5.0) + 32.0;
    }
    
    public static double fahrenheitToCelsius(double fahrenheit) {
        return (fahrenheit - 32.0) * 5.0 / 9.0;
    }
    
    public static double kilometersToMiles(double kilometers) {
        return kilometers * 0.621371;
    }
    
    public static double milesToKilometers(double miles) {
        return miles / 0.621371;
    }
    
    // Test all functions
    public static void main(String[] args) {
        System.out.println("=== Math Utilities Library Test ===\n");
        
        // Basic arithmetic
        System.out.println("Basic Arithmetic:");
        System.out.println("Add: " + add(15, 25));
        System.out.println("Add (double): " + add(15.5, 25.3));
        System.out.println("Subtract: " + subtract(50, 20));
        System.out.println("Multiply: " + multiply(6, 7));
        System.out.println("Divide: " + divide(45, 9));
        System.out.println();
        
        // Advanced math
        System.out.println("Advanced Math:");
        System.out.println("Square root of 64: " + squareRoot(64));
        System.out.println("2^8: " + power(2, 8));
        System.out.println("5!: " + factorial(5));
        System.out.println();
        
        // Number theory
        System.out.println("Number Theory:");
        System.out.println("Is 17 prime? " + isPrime(17));
        System.out.println("Is 15 prime? " + isPrime(15));
        System.out.println("GCD of 48 and 18: " + gcd(48, 18));
        System.out.println("LCM of 12 and 8: " + lcm(12, 8));
        System.out.println();
        
        // Statistics
        System.out.println("Statistics:");
        System.out.println("Average of 10,20,30,40,50: " + average(10, 20, 30, 40, 50));
        System.out.println("Max of 5,2,8,1,9: " + max(5, 2, 8, 1, 9));
        System.out.println("Min of 5,2,8,1,9: " + min(5, 2, 8, 1, 9));
        System.out.println();
        
        // Geometry
        System.out.println("Geometry:");
        System.out.printf("Circle area (radius 5): %.2f%n", circleArea(5));
        System.out.printf("Circle circumference (radius 5): %.2f%n", circleCircumference(5));
        System.out.printf("Rectangle area (4x6): %.2f%n", rectangleArea(4, 6));
        System.out.printf("Triangle area (base 8, height 5): %.2f%n", triangleArea(8, 5));
        System.out.println();
        
        // Conversions
        System.out.println("Conversions:");
        System.out.printf("25°C in Fahrenheit: %.1f°F%n", celsiusToFahrenheit(25));
        System.out.printf("77°F in Celsius: %.1f°C%n", fahrenheitToCelsius(77));
        System.out.printf("10 km in miles: %.2f miles%n", kilometersToMiles(10));
        System.out.printf("5 miles in km: %.2f km%n", milesToKilometers(5));
    }
}
```

### **Challenge 2: Text Processing Utility**

Create `TextUtilities.java`:

```java
public class TextUtilities {
    
    // Basic string operations
    public static String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }
    
    public static String capitalizeWords(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        String[] words = text.split("\\s+");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                result.append(" ");
            }
            result.append(capitalize(words[i]));
        }
        
        return result.toString();
    }
    
    public static String reverse(String text) {
        if (text == null) return null;
        
        StringBuilder reversed = new StringBuilder();
        for (int i = text.length() - 1; i >= 0; i--) {
            reversed.append(text.charAt(i));
        }
        return reversed.toString();
    }
    
    // Recursive reverse (alternative implementation)
    public static String reverseRecursive(String text) {
        if (text == null || text.length() <= 1) {
            return text;
        }
        return text.charAt(text.length() - 1) + reverseRecursive(text.substring(0, text.length() - 1));
    }
    
    // Counting functions
    public static int countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }
    
    public static int countVowels(String text) {
        if (text == null) return 0;
        
        int count = 0;
        String vowels = "aeiouAEIOU";
        
        for (int i = 0; i < text.length(); i++) {
            if (vowels.indexOf(text.charAt(i)) != -1) {
                count++;
            }
        }
        return count;
    }
    
    public static int countConsonants(String text) {
        if (text == null) return 0;
        
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isLetter(ch) && "aeiouAEIOU".indexOf(ch) == -1) {
                count++;
            }
        }
        return count;
    }
    
    // Validation functions
    public static boolean isPalindrome(String text) {
        if (text == null) return false;
        
        // Clean the text: remove spaces, punctuation, convert to lowercase
        String cleaned = text.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        return cleaned.equals(reverse(cleaned));
    }
    
    public static boolean isAnagram(String text1, String text2) {
        if (text1 == null || text2 == null) return false;
        
        // Clean both strings
        String clean1 = text1.replaceAll("[^a-zA-Z]", "").toLowerCase();
        String clean2 = text2.replaceAll("[^a-zA-Z]", "").toLowerCase();
        
        if (clean1.length() != clean2.length()) return false;
        
        // Count character frequencies
        int[] charCount = new int[26];
        
        for (int i = 0; i < clean1.length(); i++) {
            charCount[clean1.charAt(i) - 'a']++;
            charCount[clean2.charAt(i) - 'a']--;
        }
        
        for (int count : charCount) {
            if (count != 0) return false;
        }
        
        return true;
    }
    
    // Text analysis
    public static void analyzeText(String text) {
        if (text == null) {
            System.out.println("Text is null");
            return;
        }
        
        System.out.println("=== Text Analysis ===");
        System.out.println("Original text: \"" + text + "\"");
        System.out.println("Length: " + text.length());
        System.out.println("Word count: " + countWords(text));
        System.out.println("Vowel count: " + countVowels(text));
        System.out.println("Consonant count: " + countConsonants(text));
        System.out.println("Is palindrome: " + isPalindrome(text));
        System.out.println("Capitalized: \"" + capitalize(text) + "\"");
        System.out.println("Title case: \"" + capitalizeWords(text) + "\"");
        System.out.println("Reversed: \"" + reverse(text) + "\"");
        System.out.println();
    }
    
    // String formatting utilities
    public static String padLeft(String text, int totalLength, char paddingChar) {
        if (text == null) text = "";
        if (text.length() >= totalLength) return text;
        
        StringBuilder padded = new StringBuilder();
        for (int i = 0; i < totalLength - text.length(); i++) {
            padded.append(paddingChar);
        }
        padded.append(text);
        return padded.toString();
    }
    
    public static String padRight(String text, int totalLength, char paddingChar) {
        if (text == null) text = "";
        if (text.length() >= totalLength) return text;
        
        StringBuilder padded = new StringBuilder(text);
        while (padded.length() < totalLength) {
            padded.append(paddingChar);
        }
        return padded.toString();
    }
    
    public static void main(String[] args) {
        System.out.println("=== Text Utilities Library Test ===\n");
        
        // Test different texts
        String[] testTexts = {
            "hello world",
            "A man a plan a canal Panama",
            "racecar",
            "The quick brown fox jumps over the lazy dog",
            "programming"
        };
        
        for (String text : testTexts) {
            analyzeText(text);
        }
        
        // Test anagrams
        System.out.println("=== Anagram Tests ===");
        String[][] anagramPairs = {
            {"listen", "silent"},
            {"evil", "vile"},
            {"hello", "world"},
            {"astronomer", "moon starer"}
        };
        
        for (String[] pair : anagramPairs) {
            boolean isAnagram = isAnagram(pair[0], pair[1]);
            System.out.println("\"" + pair[0] + "\" and \"" + pair[1] + "\" are anagrams: " + isAnagram);
        }
        System.out.println();
        
        // Test padding
        System.out.println("=== Padding Tests ===");
        String text = "Java";
        System.out.println("Original: \"" + text + "\"");
        System.out.println("Pad left (10, '*'): \"" + padLeft(text, 10, '*') + "\"");
        System.out.println("Pad right (10, '-'): \"" + padRight(text, 10, '-') + "\"");
    }
}
```

---

## 📝 Key Takeaways from Day 4

### **Method Mastery:**
- ✅ **Method structure:** Access modifiers, return types, parameters
- ✅ **Method overloading:** Same name, different parameters
- ✅ **Variable arguments:** Using varargs (...) for flexible parameters
- ✅ **Return values:** Different types and multiple return points

### **Scope Understanding:**
- ✅ **Local scope:** Variables within methods and blocks
- ✅ **Static scope:** Class-level variables and methods
- ✅ **Instance scope:** Object-specific variables
- ✅ **Parameter scope:** Method parameters as local variables

### **Recursion Skills:**
- ✅ **Base cases:** Stopping conditions for recursion
- ✅ **Recursive cases:** How methods call themselves
- ✅ **Classic algorithms:** Factorial, Fibonacci, binary search
- ✅ **Problem-solving:** Breaking down complex problems

### **Code Organization:**
- ✅ **Utility libraries:** Creating reusable code modules
- ✅ **Static methods:** Utility functions that don't need objects
- ✅ **Method design:** Single responsibility and clear naming
- ✅ **Error handling:** Validating inputs and handling edge cases

---

## 🚀 Tomorrow's Preview (Day 5 - July 14)

**Focus:** Classes and Objects (OOP Foundation)
- Class definition and object creation
- Instance variables and methods
- Constructors (default, parameterized, overloaded)
- this keyword and object references
- Access modifiers in detail

---

## ✅ Day 4 Checklist

**Completed Tasks:**
- [ ] Create methods with various parameter types and return values
- [ ] Master method overloading for flexible functionality
- [ ] Understand variable scope (local, static, instance)
- [ ] Implement recursive algorithms (factorial, Fibonacci, etc.)
- [ ] Build comprehensive Math Utilities library
- [ ] Build Text Processing Utilities library  
- [ ] Organize code into reusable utility classes
- [ ] Commit code to GitHub: "Day 4: Methods, Scope, and Recursion mastery"

**Bonus Achievements:**
- [ ] Add more mathematical functions to utilities
- [ ] Create additional text processing methods
- [ ] Experiment with complex recursive problems
- [ ] Document your utility classes with comments

---

## 🎉 Incredible Progress!

You've now mastered the art of writing clean, reusable, and organized code! Your programs can now be broken down into logical, testable, and maintainable methods. Tomorrow we'll dive into Object-Oriented Programming - the real power of Java where you'll create your own data types and model real-world entities!

**You're becoming a true Java developer! Excellent work!** 🚀