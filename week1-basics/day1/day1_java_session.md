# Day 1: Java Foundation - Environment Setup & Basics
**Date:** July 10, 2025  
**Duration:** 1.5 hours (Combined Morning + Evening Session)  
**Focus:** Get Java running and understand core concepts

---

## 🎯 Today's Learning Goals
By the end of this session, you'll:
- ✅ Have Java development environment ready
- ✅ Understand JVM, JRE, and JDK differences
- ✅ Write and run your first Java programs
- ✅ Understand Java compilation process
- ✅ Commit your first Java code to GitHub

---

## 📚 Part 1: Theory & Setup (45 minutes)

### **Step 1: Understanding Java Ecosystem (10 minutes)**

**What is Java?**
Java is a high-level, object-oriented programming language designed for platform independence. The key principle is "Write Once, Run Anywhere" (WORA).

**Key Components:**
- **JDK (Java Development Kit):** Complete development toolkit including compiler (javac), runtime, and tools
- **JRE (Java Runtime Environment):** Runtime environment to execute Java applications  
- **JVM (Java Virtual Machine):** Virtual machine that executes Java bytecode

**The Java Process:**
```
Source Code (.java) → Compiler (javac) → Bytecode (.class) → JVM → Machine Code
```

### **Step 2: Environment Setup (15 minutes)**

**Download and Install JDK:**
1. Go to [OpenJDK](https://adoptium.net/) or [Oracle JDK](https://www.oracle.com/java/technologies/downloads/)
2. Download **JDK 17** or **JDK 21** (LTS versions)
3. Install following the platform-specific instructions
4. Verify installation:
   ```bash
   java -version
   javac -version
   ```

**Choose Your IDE:**
- **Recommended:** IntelliJ IDEA Community (Free)
- **Alternative:** Eclipse IDE, VS Code with Java Extension

**Install IntelliJ IDEA:**
1. Download from [JetBrains website](https://www.jetbrains.com/idea/download/)
2. Install Community Edition (Free)
3. Configure JDK during first startup

### **Step 3: Your First Java Program (20 minutes)**

**Understanding Java Program Structure:**
```java
// This is a comment
public class HelloWorld {           // Class declaration
    public static void main(String[] args) {  // Main method
        System.out.println("Hello, World!");  // Output statement
    }
}
```

**Key Concepts:**
- **Class:** Blueprint for objects (everything in Java is inside a class)
- **main method:** Entry point of Java application
- **public:** Access modifier (visible everywhere)
- **static:** Can be called without creating object instance
- **void:** Method returns nothing
- **String[] args:** Command-line arguments

---

## 💻 Part 2: Hands-On Practice (45 minutes)

### **Exercise 1: Create Your First Java Project (15 minutes)**

**Using IntelliJ IDEA:**
1. Create new project: `File → New → Project`
2. Choose "Java" and select your JDK
3. Project name: `JavaLearningDay1`
4. Create main class: `HelloWorld.java`

**Write your first program:**
```java
public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello, Java World!");
        System.out.println("Today is July 10, 2025");
        System.out.println("I'm starting my Java journey!");
    }
}
```

**Run the program:**
- Click green arrow next to main method
- Or use: `Run → Run 'HelloWorld'`
- Or shortcut: `Ctrl+Shift+F10` (Windows/Linux) or `Cmd+Shift+R` (Mac)

### **Exercise 2: Command Line Practice (10 minutes)**

**Compile and run from terminal:**
```bash
# Navigate to your project src folder
cd /path/to/your/project/src

# Compile Java file
javac HelloWorld.java

# Run compiled class
java HelloWorld
```

**Understanding the process:**
- `javac` creates `HelloWorld.class` (bytecode)
- `java` command runs the bytecode on JVM

### **Exercise 3: Basic Java Syntax Practice (20 minutes)**

**Create a new file: `BasicSyntax.java`**
```java
public class BasicSyntax {
    public static void main(String[] args) {
        // 1. Variables and Data Types
        int age = 25;
        double salary = 50000.50;
        String name = "Java Developer";
        boolean isLearning = true;
        
        // 2. Output different data types
        System.out.println("Name: " + name);
        System.out.println("Age: " + age);
        System.out.println("Salary: $" + salary);
        System.out.println("Currently Learning: " + isLearning);
        
        // 3. Simple calculations
        int firstNumber = 10;
        int secondNumber = 20;
        int sum = firstNumber + secondNumber;
        
        System.out.println(firstNumber + " + " + secondNumber + " = " + sum);
        
        // 4. String operations
        String greeting = "Hello";
        String target = "Java";
        String message = greeting + " " + target + "!";
        
        System.out.println("Message: " + message);
        System.out.println("Message length: " + message.length());
        System.out.println("Uppercase: " + message.toUpperCase());
    }
}
```

**Run this program and observe the output!**

---

## 🔧 Part 3: GitHub Setup & Code Commit

### **Step 1: Create GitHub Repository**
1. Go to [GitHub.com](https://github.com)
2. Create new repository: `java-learning-journey`
3. Initialize with README
4. Clone to your local machine

### **Step 2: Organize Your Code**
```
java-learning-journey/
├── week1-basics/
│   ├── HelloWorld.java
│   └── BasicSyntax.java
├── README.md
└── .gitignore
```

### **Step 3: Create .gitignore**
```gitignore
# Compiled class files
*.class

# IDE files
.idea/
*.iml
.vscode/

# OS files
.DS_Store
Thumbs.db
```

### **Step 4: Commit Your Work**
```bash
git add .
git commit -m "Day 1: First Java programs - HelloWorld and BasicSyntax"
git push origin main
```

---

## 🎯 Today's Challenges (Complete These!)

### **Challenge 1: Personal Info Display**
Create `PersonalInfo.java`:
```java
public class PersonalInfo {
    public static void main(String[] args) {
        // Display your information
        String myName = "Your Name";
        int myAge = 30;  // Your age
        String profession = "Software Engineer";
        String goal = "Become Java Expert";
        
        System.out.println("=== Personal Information ===");
        System.out.println("Name: " + myName);
        System.out.println("Age: " + myAge);
        System.out.println("Profession: " + profession);
        System.out.println("2025 Goal: " + goal);
        System.out.println("Java Learning Start Date: July 10, 2025");
    }
}
```

### **Challenge 2: Simple Calculator**
Create `SimpleCalculator.java`:
```java
public class SimpleCalculator {
    public static void main(String[] args) {
        // Basic arithmetic operations
        double num1 = 15.5;
        double num2 = 4.2;
        
        System.out.println("=== Simple Calculator ===");
        System.out.println("Number 1: " + num1);
        System.out.println("Number 2: " + num2);
        System.out.println("Addition: " + (num1 + num2));
        System.out.println("Subtraction: " + (num1 - num2));
        System.out.println("Multiplication: " + (num1 * num2));
        System.out.println("Division: " + (num1 / num2));
    }
}
```

### **Challenge 3: Experiment & Explore**
Try modifying the programs:
1. Change variable values and see output
2. Add more calculations
3. Try different data types
4. Add more print statements

---

## 📝 Key Takeaways from Today

### **Concepts Learned:**
- ✅ **JDK vs JRE vs JVM:** Development kit, runtime environment, virtual machine
- ✅ **Java Compilation:** Source code → Bytecode → Machine code
- ✅ **Class Structure:** Every Java program needs a class and main method
- ✅ **Basic Syntax:** Variables, data types, System.out.println
- ✅ **Development Workflow:** Write → Compile → Run → Debug

### **Practical Skills:**
- ✅ **Environment Setup:** JDK and IDE installation
- ✅ **IDE Usage:** Creating projects, running programs
- ✅ **Command Line:** Compiling and running Java programs
- ✅ **Version Control:** GitHub repository setup
- ✅ **Code Organization:** Project structure and file management

### **Data Types Introduced:**
- `int` - Integer numbers
- `double` - Decimal numbers  
- `String` - Text data
- `boolean` - True/false values

---

## 🚀 Tomorrow's Preview (Day 2 - July 11)

**Morning Session Focus:**
- Variables and constants in detail
- All primitive data types
- Type casting and conversion

**Evening Session Focus:**
- Operators (arithmetic, logical, comparison)
- String methods and manipulation
- Practice exercises

**Preparation for Tomorrow:**
- Review today's code
- Make sure GitHub is set up
- Have IDE ready to go

---

## ✅ Today's Checklist

**Before Ending Today's Session:**
- [x] JDK installed and verified
- [ ] IDE (IntelliJ IDEA) installed and configured
- [x] Created first Java project
- [x] Successfully ran HelloWorld program
- [x] Completed BasicSyntax program
- [x] Finished both challenge programs
- [x] GitHub repository created
- [x] Code committed and pushed to GitHub
- [x] Understood basic Java program structure

**Bonus Points:**
- [x] Experimented with different variable values
- [x] Tried running programs from command line
- [ ] Read through Oracle Java documentation
- [ ] Joined a Java learning community online

---

## 🎉 Congratulations!

You've successfully completed Day 1 of your Java learning journey! You now have:
- A working Java development environment
- Understanding of basic Java concepts
- Your first Java programs running
- A GitHub repository for tracking progress

**Tomorrow we'll dive deeper into variables, data types, and operators. Keep up the momentum!** 🚀

**Remember:** The key to mastering Java is consistent daily practice. You're off to a great start!