# Day 10: Collections Framework - Part 1 (July 19, 2025)

**Date:** July 19, 2025  
**Duration:** 2.5 hours  
**Focus:** Master Lists and Sets for dynamic data management

---

## 🎯 Today's Learning Goals

By the end of this session, you'll:
- ✅ Master List interface (ArrayList, LinkedList, Vector)
- ✅ Understand Set interface (HashSet, TreeSet, LinkedHashSet)
- ✅ Learn Iterator and enhanced for-loop patterns
- ✅ Build dynamic data structures for efficient data management
- ✅ Create applications using different collection types

---

## 📚 Part 1: Collections Framework Overview (30 minutes)

### **Understanding Collections Framework**

**Java Collections Framework**: A unified architecture for representing and manipulating collections of objects.

**Core Interfaces:**
- `Collection` - Root interface for most collection types
- `List` - Ordered collection (allows duplicates)
- `Set` - No duplicate elements
- `Queue` - Collection designed for holding elements prior to processing
- `Map` - Key-value pairs (not part of Collection hierarchy)

### **Collection Hierarchy**

```
Collection<E>
├── List<E>
│   ├── ArrayList<E>
│   ├── LinkedList<E>
│   └── Vector<E>
├── Set<E>
│   ├── HashSet<E>
│   ├── LinkedHashSet<E>
│   └── TreeSet<E>
└── Queue<E>
    ├── LinkedList<E>
    ├── PriorityQueue<E>
    └── ArrayDeque<E>
```

---

## 📋 Part 2: List Interface (45 minutes)

### **List Characteristics:**
- Ordered collection (maintains insertion order)
- Allows duplicate elements
- Elements can be accessed by index
- Resizable arrays

### **List Implementations Comparison**

| Feature | ArrayList | LinkedList | Vector |
|---------|-----------|------------|--------|
| **Access Time** | O(1) | O(n) | O(1) |
| **Insert/Delete** | O(n) | O(1) | O(n) |
| **Memory** | Efficient | Higher overhead | Efficient |
| **Thread Safety** | No | No | Yes |
| **Use Case** | Random access | Frequent modifications | Legacy/Thread-safe |

### **ArrayList Examples**

Create `ArrayListDemo.java`:

```java
import java.util.*;

/**
 * ArrayList demonstration showing common operations
 * Best for: Random access, reading operations
 */
public class ArrayListDemo {
    
    public static void main(String[] args) {
        System.out.println("=== ArrayList Demonstration ===\n");
        
        // Creating and initializing ArrayList
        demonstrateCreation();
        
        // Basic operations
        demonstrateBasicOperations();
        
        // Iteration patterns
        demonstrateIteration();
        
        // Performance considerations
        demonstratePerformance();
    }
    
    private static void demonstrateCreation() {
        System.out.println("1. ArrayList Creation:");
        
        // Different ways to create ArrayList
        ArrayList<String> fruits = new ArrayList<>();
        ArrayList<String> colors = new ArrayList<>(10); // Initial capacity
        ArrayList<String> animals = new ArrayList<>(Arrays.asList("Cat", "Dog", "Bird"));
        
        System.out.println("Empty fruits list: " + fruits);
        System.out.println("Colors with capacity 10: " + colors);
        System.out.println("Animals with initial data: " + animals);
        System.out.println();
    }
    
    private static void demonstrateBasicOperations() {
        System.out.println("2. Basic ArrayList Operations:");
        
        List<String> groceries = new ArrayList<>();
        
        // Adding elements
        groceries.add("Apples");
        groceries.add("Bananas");
        groceries.add("Carrots");
        groceries.add(1, "Bread"); // Insert at specific index
        
        System.out.println("After adding: " + groceries);
        System.out.println("Size: " + groceries.size());
        
        // Accessing elements
        System.out.println("First item: " + groceries.get(0));
        System.out.println("Last item: " + groceries.get(groceries.size() - 1));
        
        // Modifying elements
        groceries.set(0, "Green Apples");
        System.out.println("After modification: " + groceries);
        
        // Checking existence
        System.out.println("Contains Bread: " + groceries.contains("Bread"));
        System.out.println("Index of Carrots: " + groceries.indexOf("Carrots"));
        
        // Removing elements
        groceries.remove("Bananas"); // Remove by value
        groceries.remove(0); // Remove by index
        System.out.println("After removal: " + groceries);
        System.out.println();
    }
    
    private static void demonstrateIteration() {
        System.out.println("3. ArrayList Iteration Patterns:");
        
        List<Integer> numbers = Arrays.asList(10, 20, 30, 40, 50);
        
        // Enhanced for-loop (recommended for simple iteration)
        System.out.println("Enhanced for-loop:");
        for (Integer num : numbers) {
            System.out.print(num + " ");
        }
        System.out.println();
        
        // Traditional for-loop (when you need index)
        System.out.println("Traditional for-loop with index:");
        for (int i = 0; i < numbers.size(); i++) {
            System.out.println("  Index " + i + ": " + numbers.get(i));
        }
        
        // Iterator (safe for removal during iteration)
        System.out.println("Iterator pattern:");
        List<Integer> mutableNumbers = new ArrayList<>(numbers);
        Iterator<Integer> iterator = mutableNumbers.iterator();
        while (iterator.hasNext()) {
            Integer num = iterator.next();
            if (num % 20 == 0) {
                iterator.remove(); // Safe removal during iteration
                System.out.println("  Removed: " + num);
            }
        }
        System.out.println("After iterator removal: " + mutableNumbers);
        System.out.println();
    }
    
    private static void demonstratePerformance() {
        System.out.println("4. ArrayList Performance:");
        
        List<String> perfTest = new ArrayList<>();
        
        // Measure insertion time
        long startTime = System.nanoTime();
        for (int i = 0; i < 100000; i++) {
            perfTest.add("Item " + i);
        }
        long endTime = System.nanoTime();
        
        System.out.println("Added 100,000 items in: " + 
                         (endTime - startTime) / 1_000_000 + " ms");
        
        // Measure random access time
        startTime = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            String item = perfTest.get(i * 10); // Random access
        }
        endTime = System.nanoTime();
        
        System.out.println("10,000 random accesses in: " + 
                         (endTime - startTime) / 1_000_000 + " ms");
        System.out.println();
    }
}
```

### **LinkedList Examples**

Create `LinkedListDemo.java`:

```java
import java.util.*;

/**
 * LinkedList demonstration showing when to use LinkedList
 * Best for: Frequent insertions/deletions, queue operations
 */
public class LinkedListDemo {
    
    public static void main(String[] args) {
        System.out.println("=== LinkedList Demonstration ===\n");
        
        // LinkedList as List
        demonstrateListOperations();
        
        // LinkedList as Deque (Double-ended queue)
        demonstrateDequeOperations();
        
        // Performance comparison
        demonstratePerformanceComparison();
    }
    
    private static void demonstrateListOperations() {
        System.out.println("1. LinkedList as List:");
        
        LinkedList<String> tasks = new LinkedList<>();
        
        // Adding elements
        tasks.add("Write code");
        tasks.add("Test application");
        tasks.add("Deploy to production");
        tasks.addFirst("Plan features"); // Add to beginning
        tasks.addLast("Celebrate!"); // Add to end
        
        System.out.println("Task list: " + tasks);
        
        // Accessing elements (O(n) operation!)
        System.out.println("First task: " + tasks.getFirst());
        System.out.println("Last task: " + tasks.getLast());
        System.out.println("Middle task: " + tasks.get(2));
        
        // Removing elements (efficient at ends)
        String completed = tasks.removeFirst();
        System.out.println("Completed: " + completed);
        System.out.println("Remaining tasks: " + tasks);
        System.out.println();
    }
    
    private static void demonstrateDequeOperations() {
        System.out.println("2. LinkedList as Deque (Double-ended Queue):");
        
        Deque<String> browserHistory = new LinkedList<>();
        
        // Simulate browser navigation
        browserHistory.addLast("google.com");
        browserHistory.addLast("stackoverflow.com");
        browserHistory.addLast("github.com");
        browserHistory.addLast("oracle.com/java");
        
        System.out.println("Browser history: " + browserHistory);
        
        // Navigate back
        String currentPage = browserHistory.removeLast();
        System.out.println("Current page: " + currentPage);
        System.out.println("After going back: " + browserHistory);
        
        // Go to new page
        browserHistory.addLast("docs.oracle.com");
        System.out.println("After new navigation: " + browserHistory);
        
        // Check what's at both ends
        System.out.println("First visited: " + browserHistory.peekFirst());
        System.out.println("Most recent: " + browserHistory.peekLast());
        System.out.println();
    }
    
    private static void demonstratePerformanceComparison() {
        System.out.println("3. Performance Comparison: ArrayList vs LinkedList");
        
        final int SIZE = 100000;
        
        // Test insertion at beginning
        System.out.println("Inserting " + SIZE + " elements at beginning:");
        
        // ArrayList insertion at beginning (slow)
        List<Integer> arrayList = new ArrayList<>();
        long startTime = System.nanoTime();
        for (int i = 0; i < SIZE; i++) {
            arrayList.add(0, i); // Insert at beginning
        }
        long arrayListTime = System.nanoTime() - startTime;
        
        // LinkedList insertion at beginning (fast)
        List<Integer> linkedList = new LinkedList<>();
        startTime = System.nanoTime();
        for (int i = 0; i < SIZE; i++) {
            linkedList.add(0, i); // Insert at beginning
        }
        long linkedListTime = System.nanoTime() - startTime;
        
        System.out.println("ArrayList time: " + arrayListTime / 1_000_000 + " ms");
        System.out.println("LinkedList time: " + linkedListTime / 1_000_000 + " ms");
        System.out.println("LinkedList is " + (arrayListTime / linkedListTime) + "x faster for insertions at beginning");
        
        // Test random access
        System.out.println("\\nRandom access test (10,000 accesses):");
        
        startTime = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            arrayList.get(i * 10);
        }
        arrayListTime = System.nanoTime() - startTime;
        
        startTime = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            linkedList.get(i * 10);
        }
        linkedListTime = System.nanoTime() - startTime;
        
        System.out.println("ArrayList time: " + arrayListTime / 1_000_000 + " ms");
        System.out.println("LinkedList time: " + linkedListTime / 1_000_000 + " ms");
        System.out.println("ArrayList is " + (linkedListTime / arrayListTime) + "x faster for random access");
        System.out.println();
    }
}
```

---

## 🔢 Part 3: Set Interface (45 minutes)

### **Set Characteristics:**
- No duplicate elements
- Mathematical set abstraction
- Various ordering guarantees

### **Set Implementations Comparison**

| Feature | HashSet | LinkedHashSet | TreeSet |
|---------|---------|---------------|---------|
| **Ordering** | None | Insertion order | Sorted order |
| **Performance** | O(1) | O(1) | O(log n) |
| **Null Values** | One null | One null | No nulls |
| **Use Case** | Fast lookup | Ordered fast lookup | Sorted collection |

### **Set Examples**

Create `SetDemo.java`:

```java
import java.util.*;

/**
 * Comprehensive Set demonstration showing all Set implementations
 * Demonstrates duplicate elimination and different ordering behaviors
 */
public class SetDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Set Interface Demonstration ===\n");
        
        // Basic Set operations
        demonstrateBasicSetOperations();
        
        // Different Set implementations
        demonstrateSetImplementations();
        
        // Set mathematical operations
        demonstrateSetMathOperations();
        
        // Practical examples
        demonstratePracticalExamples();
    }
    
    private static void demonstrateBasicSetOperations() {
        System.out.println("1. Basic Set Operations:");
        
        Set<String> uniqueWords = new HashSet<>();
        
        // Adding elements (duplicates ignored)
        uniqueWords.add("Java");
        uniqueWords.add("Python");
        uniqueWords.add("JavaScript");
        uniqueWords.add("Java"); // Duplicate - ignored
        uniqueWords.add("Python"); // Duplicate - ignored
        
        System.out.println("Unique words: " + uniqueWords);
        System.out.println("Size: " + uniqueWords.size());
        
        // Checking existence
        System.out.println("Contains Java: " + uniqueWords.contains("Java"));
        System.out.println("Contains C++: " + uniqueWords.contains("C++"));
        
        // Removing elements
        uniqueWords.remove("JavaScript");
        System.out.println("After removal: " + uniqueWords);
        System.out.println();
    }
    
    private static void demonstrateSetImplementations() {
        System.out.println("2. Different Set Implementations:");
        
        // Same data, different implementations
        String[] languages = {"Java", "Python", "C++", "JavaScript", "Java", "Python"};
        
        // HashSet - no ordering
        Set<String> hashSet = new HashSet<>();
        Collections.addAll(hashSet, languages);
        System.out.println("HashSet (no order): " + hashSet);
        
        // LinkedHashSet - insertion order maintained
        Set<String> linkedHashSet = new LinkedHashSet<>();
        Collections.addAll(linkedHashSet, languages);
        System.out.println("LinkedHashSet (insertion order): " + linkedHashSet);
        
        // TreeSet - sorted order
        Set<String> treeSet = new TreeSet<>();
        Collections.addAll(treeSet, languages);
        System.out.println("TreeSet (sorted): " + treeSet);
        
        System.out.println();
    }
    
    private static void demonstrateSetMathOperations() {
        System.out.println("3. Set Mathematical Operations:");
        
        Set<Integer> setA = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5));
        Set<Integer> setB = new HashSet<>(Arrays.asList(4, 5, 6, 7, 8));
        
        System.out.println("Set A: " + setA);
        System.out.println("Set B: " + setB);
        
        // Union (A ∪ B)
        Set<Integer> union = new HashSet<>(setA);
        union.addAll(setB);
        System.out.println("Union (A ∪ B): " + union);
        
        // Intersection (A ∩ B)
        Set<Integer> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);
        System.out.println("Intersection (A ∩ B): " + intersection);
        
        // Difference (A - B)
        Set<Integer> difference = new HashSet<>(setA);
        difference.removeAll(setB);
        System.out.println("Difference (A - B): " + difference);
        
        // Symmetric Difference (A ⊕ B)
        Set<Integer> symmetricDiff = new HashSet<>(union);
        symmetricDiff.removeAll(intersection);
        System.out.println("Symmetric Difference (A ⊕ B): " + symmetricDiff);
        
        System.out.println();
    }
    
    private static void demonstratePracticalExamples() {
        System.out.println("4. Practical Set Examples:");
        
        // Example 1: Unique visitor tracking
        demonstrateUniqueVisitors();
        
        // Example 2: Tag management system
        demonstrateTagSystem();
        
        // Example 3: Duplicate detection
        demonstrateDuplicateDetection();
    }
    
    private static void demonstrateUniqueVisitors() {
        System.out.println("Example 1: Website Unique Visitors");
        
        Set<String> uniqueVisitors = new HashSet<>();
        String[] visitors = {"user123", "admin", "user456", "user123", "guest", "admin", "user789"};
        
        for (String visitor : visitors) {
            boolean isNewVisitor = uniqueVisitors.add(visitor);
            if (isNewVisitor) {
                System.out.println("  New visitor: " + visitor);
            } else {
                System.out.println("  Returning visitor: " + visitor);
            }
        }
        
        System.out.println("Total unique visitors: " + uniqueVisitors.size());
        System.out.println("Unique visitors: " + uniqueVisitors);
        System.out.println();
    }
    
    private static void demonstrateTagSystem() {
        System.out.println("Example 2: Blog Post Tag System");
        
        // Using TreeSet for sorted tags
        Set<String> articleTags = new TreeSet<>();
        
        articleTags.add("Java");
        articleTags.add("Programming");
        articleTags.add("Tutorial");
        articleTags.add("Beginner");
        articleTags.add("OOP");
        articleTags.add("Java"); // Duplicate ignored
        
        System.out.println("Article tags (sorted): " + articleTags);
        
        // Tag suggestions based on existing tags
        Set<String> suggestedTags = new TreeSet<>(Arrays.asList("Advanced", "Collections", "Java", "Spring"));
        suggestedTags.removeAll(articleTags); // Remove already used tags
        System.out.println("Suggested new tags: " + suggestedTags);
        System.out.println();
    }
    
    private static void demonstrateDuplicateDetection() {
        System.out.println("Example 3: Duplicate Detection in Data Processing");
        
        List<String> emailList = Arrays.asList(
            "john@example.com", "jane@example.com", "bob@example.com",
            "john@example.com", "alice@example.com", "jane@example.com"
        );
        
        System.out.println("Original email list: " + emailList);
        
        Set<String> uniqueEmails = new LinkedHashSet<>(emailList);
        System.out.println("Unique emails (order preserved): " + uniqueEmails);
        
        System.out.println("Original count: " + emailList.size());
        System.out.println("Unique count: " + uniqueEmails.size());
        System.out.println("Duplicates removed: " + (emailList.size() - uniqueEmails.size()));
        System.out.println();
    }
}
```

---

## 🎓 Part 4: Student Grade Management System Project (45 minutes)

### **Complete Student Management System**

Create `Student.java`:

```java
import java.util.*;

/**
 * Student class for Grade Management System
 * Demonstrates use of Collections in class design
 */
public class Student {
    private final String studentId;
    private final String name;
    private final String email;
    private final Map<String, List<Double>> subjectGrades; // Subject -> List of grades
    
    public Student(String studentId, String name, String email) {
        this.studentId = studentId;
        this.name = name;
        this.email = email;
        this.subjectGrades = new HashMap<>();
    }
    
    // Add grade for a subject
    public void addGrade(String subject, double grade) {
        subjectGrades.computeIfAbsent(subject, k -> new ArrayList<>()).add(grade);
    }
    
    // Get all grades for a subject
    public List<Double> getGrades(String subject) {
        return subjectGrades.getOrDefault(subject, new ArrayList<>());
    }
    
    // Calculate average grade for a subject
    public double getSubjectAverage(String subject) {
        List<Double> grades = getGrades(subject);
        if (grades.isEmpty()) {
            return 0.0;
        }
        return grades.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }
    
    // Calculate overall GPA
    public double calculateGPA() {
        if (subjectGrades.isEmpty()) {
            return 0.0;
        }
        
        double totalPoints = 0.0;
        int subjectCount = 0;
        
        for (List<Double> grades : subjectGrades.values()) {
            if (!grades.isEmpty()) {
                totalPoints += grades.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                subjectCount++;
            }
        }
        
        return subjectCount > 0 ? totalPoints / subjectCount : 0.0;
    }
    
    // Get letter grade based on GPA
    public String getLetterGrade() {
        double gpa = calculateGPA();
        if (gpa >= 90) return "A";
        if (gpa >= 80) return "B";
        if (gpa >= 70) return "C";
        if (gpa >= 60) return "D";
        return "F";
    }
    
    // Get all subjects this student is enrolled in
    public Set<String> getEnrolledSubjects() {
        return new HashSet<>(subjectGrades.keySet());
    }
    
    // Get total number of grades recorded
    public int getTotalGradesCount() {
        return subjectGrades.values().stream().mapToInt(List::size).sum();
    }
    
    // Check if student is failing any subject
    public boolean hasFailingGrades() {
        for (List<Double> grades : subjectGrades.values()) {
            if (!grades.isEmpty()) {
                double average = grades.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                if (average < 60) {
                    return true;
                }
            }
        }
        return false;
    }
    
    // Get detailed grade report
    public void printGradeReport() {
        System.out.println("\\n📋 GRADE REPORT FOR " + name.toUpperCase());
        System.out.println("=".repeat(50));
        System.out.println("Student ID: " + studentId);
        System.out.println("Email: " + email);
        System.out.println("Overall GPA: " + String.format("%.2f", calculateGPA()) + " (" + getLetterGrade() + ")");
        System.out.println();
        
        if (subjectGrades.isEmpty()) {
            System.out.println("No grades recorded.");
            return;
        }
        
        // Sort subjects for consistent display
        List<String> sortedSubjects = new ArrayList<>(subjectGrades.keySet());
        Collections.sort(sortedSubjects);
        
        for (String subject : sortedSubjects) {
            List<Double> grades = subjectGrades.get(subject);
            double average = getSubjectAverage(subject);
            
            System.out.printf("%-15s: ", subject);
            System.out.printf("Avg %.2f | Grades: ", average);
            
            for (int i = 0; i < grades.size(); i++) {
                System.out.printf("%.1f", grades.get(i));
                if (i < grades.size() - 1) System.out.print(", ");
            }
            System.out.println();
        }
        
        System.out.println("-".repeat(50));
        System.out.println("Total Grades Recorded: " + getTotalGradesCount());
        System.out.println("Subjects Enrolled: " + getEnrolledSubjects().size());
        
        if (hasFailingGrades()) {
            System.out.println("⚠️ Warning: Failing in one or more subjects");
        }
    }
    
    // Getters
    public String getStudentId() {
        return studentId;
    }
    
    public String getName() {
        return name;
    }
    
    public String getEmail() {
        return email;
    }
    
    // Override equals and hashCode for proper Set operations
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Student student = (Student) obj;
        return Objects.equals(studentId, student.studentId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(studentId);
    }
    
    @Override
    public String toString() {
        return String.format("Student[ID: %s, Name: %s, GPA: %.2f (%s), Subjects: %d]",
                           studentId, name, calculateGPA(), getLetterGrade(), 
                           getEnrolledSubjects().size());
    }
}
```

Create `StudentGradeManager.java`:

```java
import java.util.*;

/**
 * Student Grade Management System demonstrating Collections Framework
 * Uses Lists and Sets for comprehensive grade tracking and analysis
 */
public class StudentGradeManager {
    private List<Student> students;
    private Set<String> subjects;
    private Map<String, List<Double>> subjectGrades;
    
    public StudentGradeManager() {
        this.students = new ArrayList<>();
        this.subjects = new LinkedHashSet<>(); // Maintains insertion order
        this.subjectGrades = new HashMap<>();
    }
    
    // Add student
    public void addStudent(Student student) {
        if (!students.contains(student)) {
            students.add(student);
            System.out.println("✅ Added student: " + student.getName());
        } else {
            System.out.println("⚠️ Student already exists: " + student.getName());
        }
    }
    
    // Add subject
    public void addSubject(String subject) {
        if (subjects.add(subject)) {
            subjectGrades.put(subject, new ArrayList<>());
            System.out.println("✅ Added subject: " + subject);
        } else {
            System.out.println("⚠️ Subject already exists: " + subject);
        }
    }
    
    // Add grade for student in subject
    public void addGrade(String studentId, String subject, double grade) {
        Student student = findStudentById(studentId);
        if (student == null) {
            System.out.println("❌ Student not found: " + studentId);
            return;
        }
        
        if (!subjects.contains(subject)) {
            System.out.println("❌ Subject not found: " + subject);
            return;
        }
        
        if (grade < 0 || grade > 100) {
            System.out.println("❌ Invalid grade: " + grade + " (must be 0-100)");
            return;
        }
        
        student.addGrade(subject, grade);
        subjectGrades.get(subject).add(grade);
        System.out.println("✅ Added grade " + grade + " for " + student.getName() + " in " + subject);
    }
    
    // Find student by ID
    private Student findStudentById(String studentId) {
        for (Student student : students) {
            if (student.getStudentId().equals(studentId)) {
                return student;
            }
        }
        return null;
    }
    
    // Get top performing students
    public List<Student> getTopStudents(int count) {
        List<Student> sortedStudents = new ArrayList<>(students);
        
        // Sort by GPA (descending)
        sortedStudents.sort((s1, s2) -> Double.compare(s2.calculateGPA(), s1.calculateGPA()));
        
        return sortedStudents.subList(0, Math.min(count, sortedStudents.size()));
    }
    
    // Get students failing in any subject
    public Set<Student> getFailingStudents() {
        Set<Student> failingStudents = new HashSet<>();
        
        for (Student student : students) {
            for (String subject : subjects) {
                List<Double> grades = student.getGrades(subject);
                if (!grades.isEmpty()) {
                    double average = grades.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                    if (average < 60) {
                        failingStudents.add(student);
                        break;
                    }
                }
            }
        }
        
        return failingStudents;
    }
    
    // Generate class statistics
    public void generateClassStatistics() {
        System.out.println("\\n📊 CLASS STATISTICS");
        System.out.println("=".repeat(50));
        
        System.out.println("Total Students: " + students.size());
        System.out.println("Total Subjects: " + subjects.size());
        
        // Subject-wise statistics
        for (String subject : subjects) {
            List<Double> grades = subjectGrades.get(subject);
            if (!grades.isEmpty()) {
                double average = grades.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                double max = grades.stream().mapToDouble(Double::doubleValue).max().orElse(0);
                double min = grades.stream().mapToDouble(Double::doubleValue).min().orElse(0);
                
                System.out.printf("%s - Avg: %.2f, Max: %.2f, Min: %.2f%n", 
                                subject, average, max, min);
            }
        }
        
        // Overall class performance
        double classGPA = students.stream().mapToDouble(Student::calculateGPA).average().orElse(0);
        System.out.printf("Class GPA: %.2f%n", classGPA);
        
        // Top performers
        List<Student> topStudents = getTopStudents(3);
        System.out.println("\\nTop 3 Students:");
        for (int i = 0; i < topStudents.size(); i++) {
            Student student = topStudents.get(i);
            System.out.printf("%d. %s (GPA: %.2f)%n", 
                            i + 1, student.getName(), student.calculateGPA());
        }
        
        // Students needing help
        Set<Student> failingStudents = getFailingStudents();
        if (!failingStudents.isEmpty()) {
            System.out.println("\\nStudents needing assistance: " + failingStudents.size());
            for (Student student : failingStudents) {
                System.out.println("- " + student.getName());
            }
        }
    }
    
    // Demonstrate all Collection types used
    public void demonstrateCollectionUsage() {
        System.out.println("\\n🔍 COLLECTION USAGE DEMONSTRATION");
        System.out.println("=".repeat(50));
        
        System.out.println("1. List<Student> students: " + students.size() + " students");
        System.out.println("   - Maintains insertion order");
        System.out.println("   - Allows duplicates (prevented by business logic)");
        System.out.println("   - Indexed access for sorting and searching");
        
        System.out.println("\\n2. Set<String> subjects: " + subjects.size() + " subjects");
        System.out.println("   - No duplicate subjects");
        System.out.println("   - LinkedHashSet maintains insertion order");
        System.out.println("   - Subjects: " + subjects);
        
        System.out.println("\\n3. Map<String, List<Double>> subjectGrades:");
        System.out.println("   - Key-value mapping of subject to grades");
        System.out.println("   - Each value is a List<Double> for multiple grades");
        for (Map.Entry<String, List<Double>> entry : subjectGrades.entrySet()) {
            System.out.println("   - " + entry.getKey() + ": " + entry.getValue().size() + " grades");
        }
    }
    
    // Complete demo with sample data
    public static void main(String[] args) {
        System.out.println("=== Student Grade Management System Demo ===\\n");
        
        StudentGradeManager manager = new StudentGradeManager();
        
        // Add subjects
        manager.addSubject("Mathematics");
        manager.addSubject("Physics");
        manager.addSubject("Chemistry");
        manager.addSubject("English");
        
        // Add students
        manager.addStudent(new Student("S001", "Alice Johnson", "alice@school.edu"));
        manager.addStudent(new Student("S002", "Bob Smith", "bob@school.edu"));
        manager.addStudent(new Student("S003", "Carol Davis", "carol@school.edu"));
        manager.addStudent(new Student("S004", "David Wilson", "david@school.edu"));
        
        // Add grades
        String[][] gradeData = {
            {"S001", "Mathematics", "95"}, {"S001", "Mathematics", "87"}, {"S001", "Physics", "92"},
            {"S001", "Chemistry", "89"}, {"S001", "English", "94"},
            {"S002", "Mathematics", "78"}, {"S002", "Physics", "82"}, {"S002", "Chemistry", "75"},
            {"S002", "English", "88"}, {"S003", "Mathematics", "92"}, {"S003", "Physics", "95"},
            {"S003", "Chemistry", "91"}, {"S004", "Mathematics", "65"}, {"S004", "Physics", "70"},
            {"S004", "English", "72"}
        };
        
        for (String[] grade : gradeData) {
            manager.addGrade(grade[0], grade[1], Double.parseDouble(grade[2]));
        }
        
        // Generate reports
        manager.generateClassStatistics();
        manager.demonstrateCollectionUsage();
        
        // Show individual student reports
        for (Student student : manager.students) {
            student.printGradeReport();
        }
    }
    
    // Getters
    public List<Student> getStudents() {
        return new ArrayList<>(students); // Return copy to prevent external modification
    }
    
    public Set<String> getSubjects() {
        return new LinkedHashSet<>(subjects); // Return copy
    }
}
```

---

## 📝 Key Takeaways from Day 10

### **List Interface:**
- ✅ **ArrayList:** Best for random access and reading operations
- ✅ **LinkedList:** Best for frequent insertions/deletions at beginning/end
- ✅ **Vector:** Legacy synchronized version of ArrayList
- ✅ **Performance:** Choose based on your primary operations

### **Set Interface:**
- ✅ **HashSet:** Fast O(1) operations, no ordering
- ✅ **LinkedHashSet:** Fast operations with insertion order maintained
- ✅ **TreeSet:** Sorted collection with O(log n) operations
- ✅ **Duplicate Prevention:** Automatic duplicate elimination

### **Iteration Patterns:**
- ✅ **Enhanced for-loop:** Best for simple iteration
- ✅ **Iterator:** Safe for removal during iteration
- ✅ **Traditional for-loop:** When you need index access
- ✅ **Stream API:** Functional programming approach (advanced)

### **Best Practices:**
- ✅ **Interface Programming:** Use List, Set interfaces, not concrete classes
- ✅ **Capacity Planning:** Set initial capacity for better performance
- ✅ **Null Handling:** Be aware of null value policies for different implementations
- ✅ **equals() and hashCode():** Implement properly for custom objects in Sets

---

## 🎯 Today's Success Criteria

**Completed Tasks:**
- [ ] Create and manipulate different List implementations (ArrayList, LinkedList)
- [ ] Use Set collections for duplicate elimination (HashSet, TreeSet, LinkedHashSet)
- [ ] Implement proper iteration patterns for different scenarios
- [ ] Build a complete Student Grade Management System using collections
- [ ] Understand performance characteristics of different collection types
- [ ] Demonstrate mathematical set operations (union, intersection, difference)

**Bonus Achievements:**
- [ ] Compare performance between ArrayList and LinkedList for different operations
- [ ] Create custom objects that work properly with Set collections
- [ ] Implement additional collection-based features (sorting, filtering)
- [ ] Experiment with different collection combinations for complex data structures

---

## 🚀 Tomorrow's Preview (Day 11 - July 20)

**Focus:** Collections Framework Part 2
- Map interface (HashMap, TreeMap, LinkedHashMap)
- Queue and Deque interfaces
- Comparable and Comparator for custom sorting
- Building advanced data management applications

---

## 🎉 Excellent Progress!

You've mastered the fundamental collection types and built a comprehensive data management system! Lists and Sets form the foundation of efficient data handling in Java. Tomorrow we'll explore Maps and Queues for even more powerful data organization.

**Keep building with collections!** 🚀