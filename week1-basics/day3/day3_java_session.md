# Day 3: Control Flow - Making Decisions & Loops
**Date:** July 12, 2025  
**Duration:** 1.5 hours (Combined Session)  
**Focus:** if-else, switch, loops, and program flow control

---

## 🎯 Today's Learning Goals
By the end of this session, you'll:
- ✅ Master if-else statements and nested conditions
- ✅ Use switch statements (traditional and modern enhanced syntax)
- ✅ Write all types of loops (for, while, do-while, enhanced for)
- ✅ Control loop execution with break and continue
- ✅ Build interactive decision-making programs
- ✅ Create pattern-printing and number-processing applications

---

## 📚 Part 1: Decision Making with if-else (25 minutes)

### **Basic if-else Syntax**

```java
if (condition) {
    // Code to execute if condition is true
} else if (anotherCondition) {
    // Code for second condition
} else {
    // Code if all conditions are false
}
```

### **Exercise 1: Grade Calculator**

Create `GradeCalculator.java`:

```java
public class GradeCalculator {
    public static void main(String[] args) {
        System.out.println("=== Grade Calculator ===\n");
        
        // Student scores (you can change these values)
        int mathScore = 85;
        int scienceScore = 92;
        int englishScore = 78;
        int historyScore = 88;
        
        // Calculate average
        double average = (mathScore + scienceScore + englishScore + historyScore) / 4.0;
        
        System.out.println("Subject Scores:");
        System.out.println("Math: " + mathScore);
        System.out.println("Science: " + scienceScore);
        System.out.println("English: " + englishScore);
        System.out.println("History: " + historyScore);
        System.out.printf("Average: %.2f%n%n", average);
        
        // Determine letter grade using if-else
        char letterGrade;
        String performance;
        
        if (average >= 90) {
            letterGrade = 'A';
            performance = "Excellent";
        } else if (average >= 80) {
            letterGrade = 'B';
            performance = "Good";
        } else if (average >= 70) {
            letterGrade = 'C';
            performance = "Average";
        } else if (average >= 60) {
            letterGrade = 'D';
            performance = "Below Average";
        } else {
            letterGrade = 'F';
            performance = "Failing";
        }
        
        System.out.println("Grade Results:");
        System.out.println("Letter Grade: " + letterGrade);
        System.out.println("Performance: " + performance);
        
        // Additional feedback with nested conditions
        if (letterGrade == 'A') {
            System.out.println("🌟 Outstanding work! Keep it up!");
            if (average >= 95) {
                System.out.println("🏆 You're in the top tier!");
            }
        } else if (letterGrade == 'B') {
            System.out.println("👍 Good job! You're doing well!");
        } else if (letterGrade == 'C') {
            System.out.println("📚 There's room for improvement. Keep studying!");
        } else {
            System.out.println("⚠️ Consider getting extra help or tutoring.");
        }
        
        // Check for honor roll (average >= 85 AND no grade below 80)
        boolean honorRoll = average >= 85 && 
                           mathScore >= 80 && 
                           scienceScore >= 80 && 
                           englishScore >= 80 && 
                           historyScore >= 80;
        
        if (honorRoll) {
            System.out.println("🎉 Congratulations! You made the Honor Roll!");
        }
    }
}
```

### **Exercise 2: Age Category Classifier**

Create `AgeClassifier.java`:

```java
public class AgeClassifier {
    public static void main(String[] args) {
        System.out.println("=== Age Category Classifier ===\n");
        
        // Test different ages
        int[] ages = {5, 12, 16, 18, 25, 35, 50, 65, 75};
        
        for (int age : ages) {
            System.out.printf("Age %d: ", age);
            
            String category;
            String privileges;
            
            if (age < 0) {
                category = "Invalid";
                privileges = "Please enter a valid age";
            } else if (age <= 2) {
                category = "Infant";
                privileges = "Requires constant care";
            } else if (age <= 12) {
                category = "Child";
                privileges = "Elementary school, playground access";
            } else if (age <= 17) {
                category = "Teenager";
                privileges = "High school, learner's permit at 15+";
            } else if (age <= 64) {
                category = "Adult";
                privileges = "Full legal rights, can vote, work, marry";
            } else {
                category = "Senior";
                privileges = "Retirement benefits, senior discounts";
            }
            
            System.out.println(category + " - " + privileges);
        }
        
        System.out.println("\n=== Driving Eligibility Check ===");
        
        int drivingAge = 17;
        boolean hasPermit = true;
        boolean hasLicense = false;
        
        if (drivingAge >= 18) {
            System.out.println("✅ Eligible for full driving license");
        } else if (drivingAge >= 15 && hasPermit) {
            System.out.println("⚠️ Can drive with supervision (learner's permit)");
        } else if (drivingAge >= 15) {
            System.out.println("📋 Can apply for learner's permit");
        } else {
            System.out.println("❌ Too young to drive");
        }
    }
}
```

---

## 🔀 Part 2: Switch Statements (15 minutes)

### **Traditional and Enhanced Switch**

Create `SwitchDemo.java`:

```java
public class SwitchDemo {
    public static void main(String[] args) {
        System.out.println("=== Switch Statement Demo ===\n");
        
        // Traditional Switch - Day of Week
        int dayNumber = 3;
        String dayName;
        String dayType;
        
        switch (dayNumber) {
            case 1:
                dayName = "Monday";
                dayType = "Weekday";
                break;
            case 2:
                dayName = "Tuesday";
                dayType = "Weekday";
                break;
            case 3:
                dayName = "Wednesday";
                dayType = "Weekday";
                break;
            case 4:
                dayName = "Thursday";
                dayType = "Weekday";
                break;
            case 5:
                dayName = "Friday";
                dayType = "Weekday";
                break;
            case 6:
                dayName = "Saturday";
                dayType = "Weekend";
                break;
            case 7:
                dayName = "Sunday";
                dayType = "Weekend";
                break;
            default:
                dayName = "Invalid";
                dayType = "Unknown";
        }
        
        System.out.println("Traditional Switch:");
        System.out.println("Day " + dayNumber + " is " + dayName + " (" + dayType + ")");
        
        // Enhanced Switch (Java 14+) - More concise
        String monthName = switch (5) {
            case 1 -> "January";
            case 2 -> "February";
            case 3 -> "March";
            case 4 -> "April";
            case 5 -> "May";
            case 6 -> "June";
            case 7 -> "July";
            case 8 -> "August";
            case 9 -> "September";
            case 10 -> "October";
            case 11 -> "November";
            case 12 -> "December";
            default -> "Invalid Month";
        };
        
        System.out.println("\nEnhanced Switch:");
        System.out.println("Month 5 is: " + monthName);
        
        // Switch with multiple cases
        char grade = 'B';
        String feedback = switch (grade) {
            case 'A', 'a' -> "Excellent work!";
            case 'B', 'b' -> "Good job!";
            case 'C', 'c' -> "Satisfactory";
            case 'D', 'd' -> "Needs improvement";
            case 'F', 'f' -> "Failing grade";
            default -> "Invalid grade";
        };
        
        System.out.println("Grade " + grade + ": " + feedback);
        
        // Calculator using switch
        System.out.println("\n=== Simple Calculator ===");
        double num1 = 15.5;
        double num2 = 4.2;
        char operator = '*';
        
        double result = switch (operator) {
            case '+' -> num1 + num2;
            case '-' -> num1 - num2;
            case '*' -> num1 * num2;
            case '/' -> num2 != 0 ? num1 / num2 : Double.NaN;
            case '%' -> num1 % num2;
            default -> Double.NaN;
        };
        
        if (Double.isNaN(result)) {
            System.out.println("Error: Invalid operation or division by zero");
        } else {
            System.out.printf("%.2f %c %.2f = %.2f%n", num1, operator, num2, result);
        }
    }
}
```

---

## 🔄 Part 3: Loops - The Power of Repetition (35 minutes)

### **For Loops**

Create `LoopsDemo.java`:

```java
public class LoopsDemo {
    public static void main(String[] args) {
        System.out.println("=== Loops Demo ===\n");
        
        // Basic for loop
        System.out.println("1. Basic For Loop (1 to 10):");
        for (int i = 1; i <= 10; i++) {
            System.out.print(i + " ");
        }
        System.out.println("\n");
        
        // For loop with step increment
        System.out.println("2. For Loop with Step 2 (Even numbers 2-20):");
        for (int i = 2; i <= 20; i += 2) {
            System.out.print(i + " ");
        }
        System.out.println("\n");
        
        // Countdown loop
        System.out.println("3. Countdown Loop (10 to 1):");
        for (int i = 10; i >= 1; i--) {
            System.out.print(i + " ");
        }
        System.out.println("🚀\n");
        
        // Enhanced for loop (for-each) with arrays
        System.out.println("4. Enhanced For Loop (for-each):");
        String[] fruits = {"Apple", "Banana", "Orange", "Grape", "Mango"};
        for (String fruit : fruits) {
            System.out.println("I like " + fruit);
        }
        System.out.println();
        
        // Nested for loops - Multiplication table
        System.out.println("5. Nested Loops - Multiplication Table (5x5):");
        for (int i = 1; i <= 5; i++) {
            for (int j = 1; j <= 5; j++) {
                System.out.printf("%3d", i * j);
            }
            System.out.println();
        }
        System.out.println();
        
        // While loop
        System.out.println("6. While Loop - Sum of first 10 numbers:");
        int sum = 0;
        int count = 1;
        while (count <= 10) {
            sum += count;
            count++;
        }
        System.out.println("Sum of 1 to 10: " + sum);
        System.out.println();
        
        // Do-while loop
        System.out.println("7. Do-While Loop - Number guessing simulation:");
        int secretNumber = 7;
        int guess = 1;
        do {
            System.out.println("Guessing: " + guess);
            if (guess == secretNumber) {
                System.out.println("Found it! The number was " + secretNumber);
            }
            guess++;
        } while (guess <= secretNumber);
        System.out.println();
        
        // Break and Continue
        System.out.println("8. Break and Continue Demo:");
        for (int i = 1; i <= 20; i++) {
            if (i % 3 == 0) {
                continue; // Skip multiples of 3
            }
            if (i > 15) {
                break; // Stop when i > 15
            }
            System.out.print(i + " ");
        }
        System.out.println("\n(Skipped multiples of 3, stopped at 15)\n");
    }
}
```

### **Pattern Printing Programs**

Create `PatternPrinting.java`:

```java
public class PatternPrinting {
    public static void main(String[] args) {
        System.out.println("=== Pattern Printing Programs ===\n");
        
        // Pattern 1: Right triangle of stars
        System.out.println("Pattern 1: Right Triangle");
        for (int i = 1; i <= 5; i++) {
            for (int j = 1; j <= i; j++) {
                System.out.print("* ");
            }
            System.out.println();
        }
        System.out.println();
        
        // Pattern 2: Inverted right triangle
        System.out.println("Pattern 2: Inverted Triangle");
        for (int i = 5; i >= 1; i--) {
            for (int j = 1; j <= i; j++) {
                System.out.print("* ");
            }
            System.out.println();
        }
        System.out.println();
        
        // Pattern 3: Number pyramid
        System.out.println("Pattern 3: Number Pyramid");
        for (int i = 1; i <= 5; i++) {
            // Print spaces
            for (int j = 1; j <= 5 - i; j++) {
                System.out.print(" ");
            }
            // Print numbers
            for (int j = 1; j <= i; j++) {
                System.out.print(j + " ");
            }
            System.out.println();
        }
        System.out.println();
        
        // Pattern 4: Diamond pattern
        System.out.println("Pattern 4: Diamond Pattern");
        int size = 5;
        
        // Upper half
        for (int i = 1; i <= size; i++) {
            for (int j = 1; j <= size - i; j++) {
                System.out.print(" ");
            }
            for (int j = 1; j <= 2 * i - 1; j++) {
                System.out.print("*");
            }
            System.out.println();
        }
        
        // Lower half
        for (int i = size - 1; i >= 1; i--) {
            for (int j = 1; j <= size - i; j++) {
                System.out.print(" ");
            }
            for (int j = 1; j <= 2 * i - 1; j++) {
                System.out.print("*");
            }
            System.out.println();
        }
        System.out.println();
        
        // Pattern 5: Pascal's Triangle (first 6 rows)
        System.out.println("Pattern 5: Pascal's Triangle");
        for (int i = 0; i < 6; i++) {
            // Print spaces
            for (int j = 0; j < 6 - i; j++) {
                System.out.print(" ");
            }
            
            int number = 1;
            for (int j = 0; j <= i; j++) {
                System.out.print(number + " ");
                number = number * (i - j) / (j + 1);
            }
            System.out.println();
        }
    }
}
```

---

## 🎯 Today's Major Challenges

### **Challenge 1: Number Guessing Game**

Create `NumberGuessingGame.java`:

```java
public class NumberGuessingGame {
    public static void main(String[] args) {
        System.out.println("=== Number Guessing Game ===\n");
        
        // Secret number (in a real game, this would be random)
        int secretNumber = 42;
        int maxAttempts = 7;
        boolean gameWon = false;
        
        System.out.println("I'm thinking of a number between 1 and 100!");
        System.out.println("You have " + maxAttempts + " attempts to guess it.");
        System.out.println("(For demo, we'll simulate guesses)\n");
        
        // Simulate different guesses
        int[] simulatedGuesses = {25, 60, 45, 40, 43, 41, 42};
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            int guess = simulatedGuesses[attempt - 1];
            
            System.out.println("Attempt " + attempt + ": Guessing " + guess);
            
            if (guess == secretNumber) {
                System.out.println("🎉 Congratulations! You found the number!");
                System.out.println("It took you " + attempt + " attempts.");
                gameWon = true;
                break;
            } else if (guess < secretNumber) {
                System.out.println("📈 Too low! Try a higher number.");
            } else {
                System.out.println("📉 Too high! Try a lower number.");
            }
            
            // Give hints based on how close they are
            int difference = Math.abs(guess - secretNumber);
            if (difference <= 2) {
                System.out.println("🔥 You're very close!");
            } else if (difference <= 5) {
                System.out.println("🌡️ You're getting warm!");
            } else if (difference <= 10) {
                System.out.println("❄️ You're getting cold!");
            }
            
            System.out.println("Attempts remaining: " + (maxAttempts - attempt));
            System.out.println();
        }
        
        if (!gameWon) {
            System.out.println("😞 Game Over! The number was " + secretNumber);
            System.out.println("Better luck next time!");
        }
        
        // Statistics
        System.out.println("\n=== Game Statistics ===");
        System.out.println("Secret Number: " + secretNumber);
        System.out.println("Total Attempts Used: " + Math.min(maxAttempts, simulatedGuesses.length));
        System.out.println("Game Result: " + (gameWon ? "Victory! 🏆" : "Defeat 😔"));
    }
}
```

### **Challenge 2: Menu-Driven Calculator**

Create `MenuCalculator.java`:

```java
public class MenuCalculator {
    public static void main(String[] args) {
        System.out.println("=== Advanced Menu-Driven Calculator ===\n");
        
        boolean keepRunning = true;
        int operationCount = 0;
        
        // Simulate user choices (in real app, you'd get input from Scanner)
        int[] userChoices = {1, 2, 5, 7, 6, 9}; // Simulated menu selections
        double[][] operationData = {
            {15.5, 4.2},    // For addition
            {20.0, 3.0},    // For subtraction  
            {8.0, 0.0},     // For square root
            {5.0, 0.0},     // For factorial
            {12.0, 5.0},    // For power
            {0.0, 0.0}      // For exit
        };
        
        while (keepRunning && operationCount < userChoices.length) {
            // Display menu
            System.out.println("╔══════════════════════════════════╗");
            System.out.println("║        CALCULATOR MENU           ║");
            System.out.println("╠══════════════════════════════════╣");
            System.out.println("║ 1. Addition                      ║");
            System.out.println("║ 2. Subtraction                   ║");
            System.out.println("║ 3. Multiplication                ║");
            System.out.println("║ 4. Division                      ║");
            System.out.println("║ 5. Square Root                   ║");
            System.out.println("║ 6. Square                        ║");
            System.out.println("║ 7. Factorial                     ║");
            System.out.println("║ 8. Power (x^y)                   ║");
            System.out.println("║ 9. Exit                          ║");
            System.out.println("╚══════════════════════════════════╝");
            
            int choice = userChoices[operationCount];
            double num1 = operationData[operationCount][0];
            double num2 = operationData[operationCount][1];
            
            System.out.println("Selected option: " + choice);
            
            switch (choice) {
                case 1: // Addition
                    System.out.printf("%.2f + %.2f = %.2f%n", num1, num2, num1 + num2);
                    break;
                    
                case 2: // Subtraction
                    System.out.printf("%.2f - %.2f = %.2f%n", num1, num2, num1 - num2);
                    break;
                    
                case 3: // Multiplication
                    System.out.printf("%.2f × %.2f = %.2f%n", num1, num2, num1 * num2);
                    break;
                    
                case 4: // Division
                    if (num2 != 0) {
                        System.out.printf("%.2f ÷ %.2f = %.2f%n", num1, num2, num1 / num2);
                    } else {
                        System.out.println("Error: Division by zero!");
                    }
                    break;
                    
                case 5: // Square Root
                    if (num1 >= 0) {
                        System.out.printf("√%.2f = %.2f%n", num1, Math.sqrt(num1));
                    } else {
                        System.out.println("Error: Cannot calculate square root of negative number!");
                    }
                    break;
                    
                case 6: // Square
                    System.out.printf("(%.2f)² = %.2f%n", num1, num1 * num1);
                    break;
                    
                case 7: // Factorial
                    int n = (int) num1;
                    if (n >= 0 && n <= 20) { // Limit to prevent overflow
                        long factorial = 1;
                        for (int i = 1; i <= n; i++) {
                            factorial *= i;
                        }
                        System.out.printf("%d! = %d%n", n, factorial);
                    } else {
                        System.out.println("Error: Factorial only for non-negative integers ≤ 20!");
                    }
                    break;
                    
                case 8: // Power
                    double result = Math.pow(num1, num2);
                    System.out.printf("%.2f^%.2f = %.2f%n", num1, num2, result);
                    break;
                    
                case 9: // Exit
                    System.out.println("Thank you for using the calculator! 👋");
                    keepRunning = false;
                    break;
                    
                default:
                    System.out.println("Invalid choice! Please select 1-9.");
            }
            
            if (keepRunning) {
                operationCount++;
                System.out.println("\nPress Enter to continue...\n");
            }
        }
        
        System.out.println("\n=== Session Summary ===");
        System.out.println("Total operations performed: " + operationCount);
        System.out.println("Calculator session ended.");
    }
}
```

---

## 📝 Key Takeaways from Day 3

### **Decision Making:**
- ✅ **if-else chains:** Multiple conditions and nested logic
- ✅ **Boolean logic:** Combining conditions with &&, ||, !
- ✅ **Switch statements:** Traditional vs enhanced syntax
- ✅ **Fall-through behavior:** Understanding break statements

### **Loop Mastery:**
- ✅ **For loops:** Counter-controlled repetition
- ✅ **Enhanced for:** Iterating over collections/arrays
- ✅ **While loops:** Condition-controlled repetition
- ✅ **Do-while loops:** Execute at least once
- ✅ **Nested loops:** Loops within loops for complex patterns

### **Flow Control:**
- ✅ **break:** Exit loops early
- ✅ **continue:** Skip current iteration
- ✅ **Logical flow:** Designing program logic effectively

### **Practical Applications:**
- ✅ **Grade calculation** with complex conditions
- ✅ **Pattern printing** with nested loops
- ✅ **Interactive programs** with menu systems
- ✅ **Number processing** and mathematical operations

---

## 🚀 Tomorrow's Preview (Day 4 - July 13)

**Focus:** Methods & Scope
- Method declaration and calling
- Parameters and return types  
- Method overloading
- Variable scope (local, instance, class)
- Static vs non-static methods
- Recursion basics

---

## ✅ Day 3 Checklist

**Completed Tasks:**
- [ ] Master if-else statements and nested conditions
- [ ] Use both traditional and enhanced switch statements
- [ ] Write all types of loops (for, while, do-while, enhanced for)
- [ ] Control program flow with break and continue
- [ ] Create pattern printing programs
- [ ] Build Number Guessing Game
- [ ] Build Menu-Driven Calculator
- [ ] Commit code to GitHub: "Day 3: Control Flow - Conditions and Loops mastery"

**Bonus Achievements:**
- [ ] Create additional pattern designs
- [ ] Modify calculator with more operations
- [ ] Experiment with complex nested conditions
- [ ] Try different loop combinations

---

## 🎉 Outstanding Progress!

You've now mastered the fundamental building blocks of programming logic! Your programs can now make decisions, repeat actions, and handle complex workflows. Tomorrow we'll learn how to organize code into reusable methods, making your programs even more powerful and maintainable.

**You're becoming a Java developer! Keep up the excellent work!** 🚀