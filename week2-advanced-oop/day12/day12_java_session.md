# Day 12: Generics and Type Safety (July 21, 2025)

**Date:** July 21, 2025  
**Duration:** 2.5 hours  
**Focus:** Master Generics for type-safe programming and build reusable data structures

---

## 🎯 Today's Learning Goals

By the end of this session, you'll:
- ✅ Master generic classes and interfaces
- ✅ Understand bounded type parameters and wildcards
- ✅ Create generic methods and constructors
- ✅ Learn type erasure and its implications
- ✅ Build type-safe data structures and utilities
- ✅ Implement advanced generic patterns

---

## 🔤 Part 1: Generic Classes and Interfaces (45 minutes)

### **Understanding Generics**

**Generics**: Allow types (classes and interfaces) to be parameters when defining classes, interfaces, and methods.

**Benefits:**
- **Type Safety**: Compile-time type checking prevents ClassCastException
- **Elimination of Casts**: No need for explicit casting when retrieving elements
- **Generic Algorithms**: Write code that works with different types

### **Basic Generic Classes**

Create `GenericBasicsDemo.java`:

```java
import java.util.*;

/**
 * Basic Generics demonstration showing generic classes and type safety
 * Compares generic vs non-generic approaches
 */
public class GenericBasicsDemo {
    
    // Generic Box class that can hold any type
    static class Box<T> {
        private T content;
        
        public Box() {
            this.content = null;
        }
        
        public Box(T content) {
            this.content = content;
        }
        
        public void set(T content) {
            this.content = content;
        }
        
        public T get() {
            return content;
        }
        
        public boolean isEmpty() {
            return content == null;
        }
        
        public String getTypeInfo() {
            if (content == null) {
                return "Box is empty";
            }
            return "Box contains: " + content.getClass().getSimpleName() + " -> " + content;
        }
        
        @Override
        public String toString() {
            return "Box[" + (content != null ? content.toString() : "empty") + "]";
        }
    }
    
    // Generic Pair class holding two related objects
    static class Pair<T, U> {
        private T first;
        private U second;
        
        public Pair(T first, U second) {
            this.first = first;
            this.second = second;
        }
        
        public T getFirst() {
            return first;
        }
        
        public U getSecond() {
            return second;
        }
        
        public void setFirst(T first) {
            this.first = first;
        }
        
        public void setSecond(U second) {
            this.second = second;
        }
        
        // Generic method to swap the pair values
        public Pair<U, T> swap() {
            return new Pair<>(second, first);
        }
        
        @Override
        public String toString() {
            return "Pair[" + first + ", " + second + "]";
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            
            Pair<?, ?> pair = (Pair<?, ?>) obj;
            return Objects.equals(first, pair.first) && Objects.equals(second, pair.second);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(first, second);
        }
    }
    
    // Generic Triple class for three values
    static class Triple<T, U, V> {
        private T first;
        private U second;
        private V third;
        
        public Triple(T first, U second, V third) {
            this.first = first;
            this.second = second;
            this.third = third;
        }
        
        // Getters
        public T getFirst() { return first; }
        public U getSecond() { return second; }
        public V getThird() { return third; }
        
        // Setters
        public void setFirst(T first) { this.first = first; }
        public void setSecond(U second) { this.second = second; }
        public void setThird(V third) { this.third = third; }
        
        @Override
        public String toString() {
            return "Triple[" + first + ", " + second + ", " + third + "]";
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Generic Classes Demonstration ===\n");
        
        // Box demonstrations
        demonstrateBoxUsage();
        
        // Pair demonstrations
        demonstratePairUsage();
        
        // Triple demonstrations
        demonstrateTripleUsage();
        
        // Type safety comparison
        demonstrateTypeSafety();
    }
    
    private static void demonstrateBoxUsage() {
        System.out.println("1. Generic Box Class:");
        
        // String box
        Box<String> stringBox = new Box<>("Hello, Generics!");
        System.out.println("String box: " + stringBox.getTypeInfo());
        
        // Integer box
        Box<Integer> intBox = new Box<>(42);
        System.out.println("Integer box: " + intBox.getTypeInfo());
        
        // Boolean box
        Box<Boolean> boolBox = new Box<>();
        boolBox.set(true);
        System.out.println("Boolean box: " + boolBox.getTypeInfo());
        
        // List box
        Box<List<String>> listBox = new Box<>(Arrays.asList("Java", "Python", "JavaScript"));
        System.out.println("List box: " + listBox.getTypeInfo());
        
        // Empty box
        Box<Double> emptyBox = new Box<>();
        System.out.println("Empty box: " + emptyBox.getTypeInfo());
        
        System.out.println();
    }
    
    private static void demonstratePairUsage() {
        System.out.println("2. Generic Pair Class:");
        
        // Name and age pair
        Pair<String, Integer> person = new Pair<>("Alice", 28);
        System.out.println("Person: " + person);
        
        // Coordinates pair
        Pair<Double, Double> coordinates = new Pair<>(23.5, 45.7);
        System.out.println("Coordinates: " + coordinates);
        
        // Key-value pair
        Pair<String, Boolean> setting = new Pair<>("debugMode", true);
        System.out.println("Setting: " + setting);
        
        // Swap demonstration
        Pair<Integer, String> swapped = person.swap();
        System.out.println("Original: " + person);
        System.out.println("Swapped: " + swapped);
        
        // Nested pairs
        Pair<String, Pair<Integer, Integer>> nestedPair = 
            new Pair<>("Range", new Pair<>(1, 100));
        System.out.println("Nested pair: " + nestedPair);
        
        System.out.println();
    }
    
    private static void demonstrateTripleUsage() {
        System.out.println("3. Generic Triple Class:");
        
        // RGB color values
        Triple<Integer, Integer, Integer> rgbColor = new Triple<>(255, 128, 64);
        System.out.println("RGB Color: " + rgbColor);
        
        // Student info
        Triple<String, Integer, String> student = new Triple<>("Bob", 20, "Computer Science");
        System.out.println("Student: " + student);
        
        // Mixed types
        Triple<String, Double, Boolean> mixedData = new Triple<>("Temperature", 23.5, true);
        System.out.println("Mixed data: " + mixedData);
        
        System.out.println();
    }
    
    private static void demonstrateTypeSafety() {
        System.out.println("4. Type Safety Comparison:");
        
        // Without generics (old way - NOT RECOMMENDED)
        System.out.println("Without generics (unsafe):");
        List rawList = new ArrayList();
        rawList.add("String");
        rawList.add(123);
        rawList.add(true);
        
        System.out.println("Raw list: " + rawList);
        // This would cause ClassCastException at runtime
        // String str = (String) rawList.get(1); // Dangerous!
        
        // With generics (type-safe)
        System.out.println("\nWith generics (type-safe):");
        List<String> stringList = new ArrayList<>();
        stringList.add("Hello");
        stringList.add("World");
        // stringList.add(123); // Compile-time error!
        
        // Safe retrieval without casting
        String first = stringList.get(0);
        String second = stringList.get(1);
        System.out.println("Type-safe retrieval: " + first + " " + second);
        
        // Generic collections
        Map<String, Integer> scores = new HashMap<>();
        scores.put("Alice", 95);
        scores.put("Bob", 87);
        
        System.out.println("Type-safe map:");
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            System.out.printf("  %s: %d%n", entry.getKey(), entry.getValue());
        }
        
        System.out.println();
    }
}
```

---

## 🎯 Part 2: Generic Methods and Bounded Types (45 minutes)

### **Generic Methods and Bounded Type Parameters**

Create `GenericMethodsDemo.java`:

```java
import java.util.*;

/**
 * Generic Methods and Bounded Types demonstration
 * Shows how to create flexible, reusable methods with type constraints
 */
public class GenericMethodsDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Generic Methods Demonstration ===\n");
        
        // Generic utility methods
        demonstrateGenericUtilityMethods();
        
        // Bounded type parameters
        demonstrateBoundedTypes();
        
        // Multiple bounds
        demonstrateMultipleBounds();
        
        // Generic constructors
        demonstrateGenericConstructors();
    }
    
    // Generic method to swap two elements in an array
    public static <T> void swap(T[] array, int i, int j) {
        if (i >= 0 && i < array.length && j >= 0 && j < array.length) {
            T temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
    }
    
    // Generic method to find maximum element
    public static <T extends Comparable<T>> T findMax(T[] array) {
        if (array == null || array.length == 0) {
            return null;
        }
        
        T max = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i].compareTo(max) > 0) {
                max = array[i];
            }
        }
        return max;
    }
    
    // Generic method to find minimum element
    public static <T extends Comparable<T>> T findMin(T[] array) {
        if (array == null || array.length == 0) {
            return null;
        }
        
        T min = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i].compareTo(min) < 0) {
                min = array[i];
            }
        }
        return min;
    }
    
    // Generic method to convert array to list
    public static <T> List<T> arrayToList(T[] array) {
        List<T> list = new ArrayList<>();
        for (T element : array) {
            list.add(element);
        }
        return list;
    }
    
    // Generic method to reverse a list
    public static <T> void reverseList(List<T> list) {
        int size = list.size();
        for (int i = 0; i < size / 2; i++) {
            T temp = list.get(i);
            list.set(i, list.get(size - 1 - i));
            list.set(size - 1 - i, temp);
        }
    }
    
    // Generic method with multiple type parameters
    public static <T, U> Pair<T, U> createPair(T first, U second) {
        return new Pair<>(first, second);
    }
    
    // Generic method for numeric operations (bounded)
    public static <T extends Number> double calculateAverage(T[] numbers) {
        if (numbers == null || numbers.length == 0) {
            return 0.0;
        }
        
        double sum = 0.0;
        for (T number : numbers) {
            sum += number.doubleValue();
        }
        return sum / numbers.length;
    }
    
    // Generic method with upper bound for comparison
    public static <T extends Comparable<T>> boolean isInRange(T value, T min, T max) {
        return value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
    }
    
    private static void demonstrateGenericUtilityMethods() {
        System.out.println("1. Generic Utility Methods:");
        
        // String array operations
        String[] words = {"Java", "Python", "JavaScript", "C++", "Go"};
        System.out.println("Original words: " + Arrays.toString(words));
        
        swap(words, 0, 4);
        System.out.println("After swapping first and last: " + Arrays.toString(words));
        
        String maxWord = findMax(words);
        String minWord = findMin(words);
        System.out.println("Max word (alphabetically): " + maxWord);
        System.out.println("Min word (alphabetically): " + minWord);
        
        // Integer array operations
        Integer[] numbers = {42, 17, 89, 3, 56, 91, 12};
        System.out.println("\nOriginal numbers: " + Arrays.toString(numbers));
        
        Integer maxNum = findMax(numbers);
        Integer minNum = findMin(numbers);
        System.out.println("Max number: " + maxNum);
        System.out.println("Min number: " + minNum);
        
        // Convert to list and reverse
        List<Integer> numberList = arrayToList(numbers);
        System.out.println("As list: " + numberList);
        
        reverseList(numberList);
        System.out.println("Reversed list: " + numberList);
        
        // Create pairs
        Pair<String, Integer> wordLengthPair = createPair("Programming", 11);
        Pair<Double, Boolean> scoreValidPair = createPair(95.5, true);
        System.out.println("Word-length pair: " + wordLengthPair);
        System.out.println("Score-valid pair: " + scoreValidPair);
        
        System.out.println();
    }
    
    private static void demonstrateBoundedTypes() {
        System.out.println("2. Bounded Type Parameters:");
        
        // Numeric operations with bounded types
        Integer[] integers = {10, 20, 30, 40, 50};
        Double[] doubles = {1.5, 2.7, 3.9, 4.2, 5.8};
        Float[] floats = {1.1f, 2.2f, 3.3f, 4.4f, 5.5f};
        
        System.out.println("Integer array: " + Arrays.toString(integers));
        System.out.println("Average: " + calculateAverage(integers));
        
        System.out.println("Double array: " + Arrays.toString(doubles));
        System.out.println("Average: " + calculateAverage(doubles));
        
        System.out.println("Float array: " + Arrays.toString(floats));
        System.out.println("Average: " + calculateAverage(floats));
        
        // Range checking with Comparable bounds
        System.out.println("\nRange checking:");
        System.out.println("Is 25 in range [10, 50]? " + isInRange(25, 10, 50));
        System.out.println("Is 75 in range [10, 50]? " + isInRange(75, 10, 50));
        System.out.println("Is 'Java' in range ['C++', 'Python']? " + 
                         isInRange("Java", "C++", "Python"));
        
        System.out.println();
    }
    
    // Class with multiple bounds
    interface Printable {
        void print();
    }
    
    static class PrintableNumber extends Number implements Printable {
        private double value;
        
        public PrintableNumber(double value) {
            this.value = value;
        }
        
        @Override
        public void print() {
            System.out.println("PrintableNumber: " + value);
        }
        
        @Override
        public int intValue() { return (int) value; }
        @Override
        public long longValue() { return (long) value; }
        @Override
        public float floatValue() { return (float) value; }
        @Override
        public double doubleValue() { return value; }
        
        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }
    
    // Generic method with multiple bounds
    public static <T extends Number & Printable> void processAndPrint(T value) {
        System.out.println("Processing: " + value.doubleValue());
        value.print();
    }
    
    private static void demonstrateMultipleBounds() {
        System.out.println("3. Multiple Type Bounds:");
        
        PrintableNumber num1 = new PrintableNumber(42.5);
        PrintableNumber num2 = new PrintableNumber(17.8);
        
        System.out.println("Processing printable numbers:");
        processAndPrint(num1);
        processAndPrint(num2);
        
        System.out.println();
    }
    
    // Generic constructor example
    static class GenericConstructorExample<T> {
        private T value;
        private String description;
        
        // Generic constructor
        public <U> GenericConstructorExample(T value, U descriptionSource) {
            this.value = value;
            this.description = "Description from " + descriptionSource.getClass().getSimpleName() + ": " + descriptionSource.toString();
        }
        
        public T getValue() {
            return value;
        }
        
        public String getDescription() {
            return description;
        }
        
        @Override
        public String toString() {
            return "GenericConstructorExample[value=" + value + ", description=" + description + "]";
        }
    }
    
    private static void demonstrateGenericConstructors() {
        System.out.println("4. Generic Constructors:");
        
        // Different ways to use generic constructor
        GenericConstructorExample<String> example1 = 
            new GenericConstructorExample<>("Hello", 42);
        
        GenericConstructorExample<Integer> example2 = 
            new GenericConstructorExample<>(100, Arrays.asList("a", "b", "c"));
        
        GenericConstructorExample<Boolean> example3 = 
            new GenericConstructorExample<>(true, new Date());
        
        System.out.println("Example 1: " + example1);
        System.out.println("Example 2: " + example2);
        System.out.println("Example 3: " + example3);
        
        System.out.println();
    }
    
    // Helper Pair class (reused from previous example)
    static class Pair<T, U> {
        private T first;
        private U second;
        
        public Pair(T first, U second) {
            this.first = first;
            this.second = second;
        }
        
        public T getFirst() { return first; }
        public U getSecond() { return second; }
        
        @Override
        public String toString() {
            return "Pair[" + first + ", " + second + "]";
        }
    }
}
```

---

## 🌟 Part 3: Wildcards and Advanced Patterns (45 minutes)

### **Understanding Wildcards**

Create `WildcardsDemo.java`:

```java
import java.util.*;

/**
 * Wildcards demonstration showing upper bounds, lower bounds, and unbounded wildcards
 * Demonstrates PECS principle (Producer Extends, Consumer Super)
 */
public class WildcardsDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Wildcards Demonstration ===\n");
        
        // Upper bounded wildcards
        demonstrateUpperBoundedWildcards();
        
        // Lower bounded wildcards
        demonstrateLowerBoundedWildcards();
        
        // Unbounded wildcards
        demonstrateUnboundedWildcards();
        
        // PECS principle
        demonstratePECSPrinciple();
        
        // Wildcard capture
        demonstrateWildcardCapture();
    }
    
    // Upper bounded wildcard - can READ from collection
    public static double calculateTotalValue(List<? extends Number> numbers) {
        double total = 0.0;
        for (Number number : numbers) {
            total += number.doubleValue();
        }
        return total;
    }
    
    // Upper bounded wildcard for comparison
    public static <T extends Comparable<T>> T findMaximum(List<? extends T> list) {
        if (list.isEmpty()) {
            return null;
        }
        
        T max = list.get(0);
        for (T item : list) {
            if (item.compareTo(max) > 0) {
                max = item;
            }
        }
        return max;
    }
    
    // Lower bounded wildcard - can ADD to collection
    public static void addNumbers(List<? super Integer> numbers) {
        numbers.add(1);
        numbers.add(2);
        numbers.add(3);
        System.out.println("Added numbers 1, 2, 3 to the collection");
    }
    
    // Lower bounded wildcard for copying
    public static <T> void copyAll(List<? extends T> source, List<? super T> destination) {
        for (T item : source) {
            destination.add(item);
        }
    }
    
    // Unbounded wildcard
    public static void printList(List<?> list) {
        System.out.print("List contents: [");
        for (int i = 0; i < list.size(); i++) {
            System.out.print(list.get(i));
            if (i < list.size() - 1) {
                System.out.print(", ");
            }
        }
        System.out.println("]");
    }
    
    // Unbounded wildcard for size operations
    public static int getTotalSize(List<List<?>> listOfLists) {
        int total = 0;
        for (List<?> list : listOfLists) {
            total += list.size();
        }
        return total;
    }
    
    private static void demonstrateUpperBoundedWildcards() {
        System.out.println("1. Upper Bounded Wildcards (? extends Type):");
        
        // Create different types of number lists
        List<Integer> integers = Arrays.asList(1, 2, 3, 4, 5);
        List<Double> doubles = Arrays.asList(1.1, 2.2, 3.3, 4.4, 5.5);
        List<Float> floats = Arrays.asList(1.0f, 2.0f, 3.0f);
        
        // All can be passed to method expecting List<? extends Number>
        System.out.println("Integer list total: " + calculateTotalValue(integers));
        System.out.println("Double list total: " + calculateTotalValue(doubles));
        System.out.println("Float list total: " + calculateTotalValue(floats));
        
        // Finding maximum with upper bounded wildcards
        List<String> words = Arrays.asList("apple", "banana", "cherry", "date");
        String maxWord = findMaximum(words);
        System.out.println("Maximum word: " + maxWord);
        
        Integer maxInt = findMaximum(integers);
        System.out.println("Maximum integer: " + maxInt);
        
        System.out.println();
    }
    
    private static void demonstrateLowerBoundedWildcards() {
        System.out.println("2. Lower Bounded Wildcards (? super Type):");
        
        // Lists that can accept Integer or its supertypes
        List<Integer> integerList = new ArrayList<>();
        List<Number> numberList = new ArrayList<>();
        List<Object> objectList = new ArrayList<>();
        
        System.out.println("Adding to Integer list:");
        addNumbers(integerList);
        System.out.println("Integer list: " + integerList);
        
        System.out.println("Adding to Number list:");
        addNumbers(numberList);
        System.out.println("Number list: " + numberList);
        
        System.out.println("Adding to Object list:");
        addNumbers(objectList);
        System.out.println("Object list: " + objectList);
        
        // Copying with bounded wildcards
        List<String> sourceWords = Arrays.asList("hello", "world");
        List<Object> destinationObjects = new ArrayList<>();
        
        System.out.println("Copying strings to object list:");
        copyAll(sourceWords, destinationObjects);
        System.out.println("Destination: " + destinationObjects);
        
        System.out.println();
    }
    
    private static void demonstrateUnboundedWildcards() {
        System.out.println("3. Unbounded Wildcards (?):");
        
        // Different types of lists
        List<String> stringList = Arrays.asList("Java", "Python", "JavaScript");
        List<Integer> intList = Arrays.asList(10, 20, 30);
        List<Boolean> boolList = Arrays.asList(true, false, true);
        
        // All can be printed with unbounded wildcard
        printList(stringList);
        printList(intList);
        printList(boolList);
        
        // List of lists with different types
        List<List<?>> mixedLists = Arrays.asList(stringList, intList, boolList);
        System.out.println("Total elements across all lists: " + getTotalSize(mixedLists));
        
        System.out.println();
    }
    
    // PECS demonstration classes
    static class Animal {
        private String name;
        
        public Animal(String name) {
            this.name = name;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
    static class Dog extends Animal {
        public Dog(String name) {
            super("Dog:" + name);
        }
    }
    
    static class Cat extends Animal {
        public Cat(String name) {
            super("Cat:" + name);
        }
    }
    
    // PECS: Producer Extends, Consumer Super
    public static void feedAnimals(List<? extends Animal> animals) {
        // Can READ (produce) animals from the list
        for (Animal animal : animals) {
            System.out.println("Feeding " + animal);
        }
        // Cannot ADD to the list (compilation error):
        // animals.add(new Dog("Rex")); // Error!
    }
    
    public static void addAnimalsToShelter(List<? super Dog> shelter) {
        // Can ADD (consume) dogs to the shelter
        shelter.add(new Dog("Rex"));
        shelter.add(new Dog("Buddy"));
        System.out.println("Added dogs to shelter");
        
        // Cannot READ specific types (only Object):
        // Dog firstDog = shelter.get(0); // Error!
        Object firstItem = shelter.get(0); // OK, but only as Object
    }
    
    private static void demonstratePECSPrinciple() {
        System.out.println("4. PECS Principle (Producer Extends, Consumer Super):");
        
        // Producer Extends - can read from collection
        List<Dog> dogs = Arrays.asList(new Dog("Max"), new Dog("Bella"));
        List<Cat> cats = Arrays.asList(new Cat("Whiskers"), new Cat("Shadow"));
        
        System.out.println("Feeding dogs:");
        feedAnimals(dogs); // Dogs are Animals, so this works
        
        System.out.println("Feeding cats:");
        feedAnimals(cats); // Cats are Animals, so this works
        
        // Consumer Super - can add to collection
        List<Animal> animalShelter = new ArrayList<>();
        List<Object> objectShelter = new ArrayList<>();
        
        System.out.println("Adding to animal shelter:");
        addAnimalsToShelter(animalShelter);
        System.out.println("Animal shelter: " + animalShelter);
        
        System.out.println("Adding to object shelter:");
        addAnimalsToShelter(objectShelter);
        System.out.println("Object shelter: " + objectShelter);
        
        System.out.println();
    }
    
    // Wildcard capture helper
    private static <T> void swapHelper(List<T> list, int i, int j) {
        T temp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, temp);
    }
    
    // Wildcard capture method
    public static void swap(List<?> list, int i, int j) {
        swapHelper(list, i, j); // Capture the wildcard
    }
    
    private static void demonstrateWildcardCapture() {
        System.out.println("5. Wildcard Capture:");
        
        List<String> wordList = new ArrayList<>(Arrays.asList("first", "second", "third"));
        System.out.println("Before swap: " + wordList);
        
        swap(wordList, 0, 2); // Wildcard capture in action
        System.out.println("After swap: " + wordList);
        
        List<Integer> numList = new ArrayList<>(Arrays.asList(10, 20, 30));
        System.out.println("Before swap: " + numList);
        
        swap(numList, 1, 2);
        System.out.println("After swap: " + numList);
        
        System.out.println();
    }
}
```

---

## 🏗️ Part 4: Generic Data Structures (45 minutes)

### **Building Custom Generic Data Structures**

Create `GenericDataStructures.java`:

```java
import java.util.*;
import java.util.function.Consumer;

/**
 * Custom Generic Data Structures demonstration
 * Shows how to build type-safe, reusable data structures
 */
public class GenericDataStructures {
    
    // Generic Stack implementation
    static class GenericStack<T> {
        private List<T> items;
        private int maxSize;
        
        public GenericStack() {
            this(Integer.MAX_VALUE);
        }
        
        public GenericStack(int maxSize) {
            this.items = new ArrayList<>();
            this.maxSize = maxSize;
        }
        
        public void push(T item) {
            if (items.size() >= maxSize) {
                throw new IllegalStateException("Stack overflow - maximum size " + maxSize + " reached");
            }
            items.add(item);
        }
        
        public T pop() {
            if (isEmpty()) {
                throw new IllegalStateException("Cannot pop from empty stack");
            }
            return items.remove(items.size() - 1);
        }
        
        public T peek() {
            if (isEmpty()) {
                return null;
            }
            return items.get(items.size() - 1);
        }
        
        public boolean isEmpty() {
            return items.isEmpty();
        }
        
        public int size() {
            return items.size();
        }
        
        public void clear() {
            items.clear();
        }
        
        @Override
        public String toString() {
            return "Stack" + items.toString();
        }
    }
    
    // Generic Queue implementation
    static class GenericQueue<T> {
        private LinkedList<T> items;
        private int maxSize;
        
        public GenericQueue() {
            this(Integer.MAX_VALUE);
        }
        
        public GenericQueue(int maxSize) {
            this.items = new LinkedList<>();
            this.maxSize = maxSize;
        }
        
        public void enqueue(T item) {
            if (items.size() >= maxSize) {
                throw new IllegalStateException("Queue overflow - maximum size " + maxSize + " reached");
            }
            items.addLast(item);
        }
        
        public T dequeue() {
            if (isEmpty()) {
                throw new IllegalStateException("Cannot dequeue from empty queue");
            }
            return items.removeFirst();
        }
        
        public T front() {
            if (isEmpty()) {
                return null;
            }
            return items.getFirst();
        }
        
        public boolean isEmpty() {
            return items.isEmpty();
        }
        
        public int size() {
            return items.size();
        }
        
        public void clear() {
            items.clear();
        }
        
        @Override
        public String toString() {
            return "Queue" + items.toString();
        }
    }
    
    // Generic Binary Tree Node
    static class BinaryTreeNode<T extends Comparable<T>> {
        private T data;
        private BinaryTreeNode<T> left;
        private BinaryTreeNode<T> right;
        
        public BinaryTreeNode(T data) {
            this.data = data;
            this.left = null;
            this.right = null;
        }
        
        // Getters and setters
        public T getData() { return data; }
        public BinaryTreeNode<T> getLeft() { return left; }
        public BinaryTreeNode<T> getRight() { return right; }
        
        public void setLeft(BinaryTreeNode<T> left) { this.left = left; }
        public void setRight(BinaryTreeNode<T> right) { this.right = right; }
        
        @Override
        public String toString() {
            return data.toString();
        }
    }
    
    // Generic Binary Search Tree
    static class BinarySearchTree<T extends Comparable<T>> {
        private BinaryTreeNode<T> root;
        private int size;
        
        public BinarySearchTree() {
            this.root = null;
            this.size = 0;
        }
        
        public void insert(T data) {
            root = insertRecursive(root, data);
            size++;
        }
        
        private BinaryTreeNode<T> insertRecursive(BinaryTreeNode<T> node, T data) {
            if (node == null) {
                return new BinaryTreeNode<>(data);
            }
            
            int comparison = data.compareTo(node.getData());
            if (comparison < 0) {
                node.setLeft(insertRecursive(node.getLeft(), data));
            } else if (comparison > 0) {
                node.setRight(insertRecursive(node.getRight(), data));
            }
            // Equal values are ignored (no duplicates)
            
            return node;
        }
        
        public boolean search(T data) {
            return searchRecursive(root, data);
        }
        
        private boolean searchRecursive(BinaryTreeNode<T> node, T data) {
            if (node == null) {
                return false;
            }
            
            int comparison = data.compareTo(node.getData());
            if (comparison == 0) {
                return true;
            } else if (comparison < 0) {
                return searchRecursive(node.getLeft(), data);
            } else {
                return searchRecursive(node.getRight(), data);
            }
        }
        
        public void inorderTraversal(Consumer<T> action) {
            inorderTraversalRecursive(root, action);
        }
        
        private void inorderTraversalRecursive(BinaryTreeNode<T> node, Consumer<T> action) {
            if (node != null) {
                inorderTraversalRecursive(node.getLeft(), action);
                action.accept(node.getData());
                inorderTraversalRecursive(node.getRight(), action);
            }
        }
        
        public List<T> toSortedList() {
            List<T> result = new ArrayList<>();
            inorderTraversal(result::add);
            return result;
        }
        
        public int size() {
            return size;
        }
        
        public boolean isEmpty() {
            return root == null;
        }
    }
    
    // Generic Pair with utilities
    static class GenericPair<T, U> {
        private T first;
        private U second;
        
        public GenericPair(T first, U second) {
            this.first = first;
            this.second = second;
        }
        
        public T getFirst() { return first; }
        public U getSecond() { return second; }
        
        public void setFirst(T first) { this.first = first; }
        public void setSecond(U second) { this.second = second; }
        
        // Generic method to create pairs
        public static <T, U> GenericPair<T, U> of(T first, U second) {
            return new GenericPair<>(first, second);
        }
        
        // Swap the pair
        public GenericPair<U, T> swap() {
            return new GenericPair<>(second, first);
        }
        
        // Apply functions to both elements
        public <V, W> GenericPair<V, W> map(java.util.function.Function<T, V> firstMapper,
                                           java.util.function.Function<U, W> secondMapper) {
            return new GenericPair<>(firstMapper.apply(first), secondMapper.apply(second));
        }
        
        @Override
        public String toString() {
            return "(" + first + ", " + second + ")";
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            
            GenericPair<?, ?> pair = (GenericPair<?, ?>) obj;
            return Objects.equals(first, pair.first) && Objects.equals(second, pair.second);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(first, second);
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Generic Data Structures Demonstration ===\n");
        
        // Stack demonstration
        demonstrateGenericStack();
        
        // Queue demonstration
        demonstrateGenericQueue();
        
        // Binary Search Tree demonstration
        demonstrateBinarySearchTree();
        
        // Generic Pair utilities
        demonstrateGenericPairUtilities();
    }
    
    private static void demonstrateGenericStack() {
        System.out.println("1. Generic Stack:");
        
        GenericStack<String> stringStack = new GenericStack<>(5);
        
        // Push operations
        stringStack.push("First");
        stringStack.push("Second");
        stringStack.push("Third");
        
        System.out.println("Stack after pushes: " + stringStack);
        System.out.println("Peek: " + stringStack.peek());
        System.out.println("Stack size: " + stringStack.size());
        
        // Pop operations
        System.out.println("Popping elements:");
        while (!stringStack.isEmpty()) {
            System.out.println("  Popped: " + stringStack.pop());
        }
        
        System.out.println("Stack is empty: " + stringStack.isEmpty());
        
        // Integer stack
        GenericStack<Integer> intStack = new GenericStack<>();
        intStack.push(10);
        intStack.push(20);
        intStack.push(30);
        
        System.out.println("Integer stack: " + intStack);
        
        System.out.println();
    }
    
    private static void demonstrateGenericQueue() {
        System.out.println("2. Generic Queue:");
        
        GenericQueue<String> taskQueue = new GenericQueue<>(10);
        
        // Enqueue operations
        taskQueue.enqueue("Task 1");
        taskQueue.enqueue("Task 2");
        taskQueue.enqueue("Task 3");
        taskQueue.enqueue("Task 4");
        
        System.out.println("Queue after enqueues: " + taskQueue);
        System.out.println("Front element: " + taskQueue.front());
        System.out.println("Queue size: " + taskQueue.size());
        
        // Dequeue operations
        System.out.println("Processing tasks:");
        while (!taskQueue.isEmpty()) {
            System.out.println("  Processing: " + taskQueue.dequeue());
            System.out.println("  Remaining: " + taskQueue);
        }
        
        // Different type queue
        GenericQueue<Double> priceQueue = new GenericQueue<>();
        priceQueue.enqueue(19.99);
        priceQueue.enqueue(29.99);
        priceQueue.enqueue(39.99);
        
        System.out.println("Price queue: " + priceQueue);
        
        System.out.println();
    }
    
    private static void demonstrateBinarySearchTree() {
        System.out.println("3. Generic Binary Search Tree:");
        
        // Integer BST
        BinarySearchTree<Integer> intTree = new BinarySearchTree<>();
        
        int[] numbers = {50, 30, 70, 20, 40, 60, 80, 10, 25, 35, 45};
        System.out.println("Inserting numbers: " + Arrays.toString(numbers));
        
        for (int num : numbers) {
            intTree.insert(num);
        }
        
        System.out.println("Tree size: " + intTree.size());
        
        // Search operations
        System.out.println("Search results:");
        System.out.println("  Contains 40: " + intTree.search(40));
        System.out.println("  Contains 55: " + intTree.search(55));
        System.out.println("  Contains 80: " + intTree.search(80));
        
        // In-order traversal (sorted)
        System.out.print("In-order traversal: ");
        intTree.inorderTraversal(num -> System.out.print(num + " "));
        System.out.println();
        
        List<Integer> sortedList = intTree.toSortedList();
        System.out.println("As sorted list: " + sortedList);
        
        // String BST
        BinarySearchTree<String> stringTree = new BinarySearchTree<>();
        String[] words = {"java", "python", "javascript", "c++", "go", "rust", "kotlin"};
        
        System.out.println("\nInserting words: " + Arrays.toString(words));
        for (String word : words) {
            stringTree.insert(word);
        }
        
        System.out.print("Words in alphabetical order: ");
        stringTree.inorderTraversal(word -> System.out.print(word + " "));
        System.out.println();
        
        System.out.println();
    }
    
    private static void demonstrateGenericPairUtilities() {
        System.out.println("4. Generic Pair Utilities:");
        
        // Create pairs
        GenericPair<String, Integer> nameAge = GenericPair.of("Alice", 28);
        GenericPair<Double, Double> coordinates = GenericPair.of(12.5, 34.7);
        GenericPair<Boolean, String> flagMessage = GenericPair.of(true, "Success");
        
        System.out.println("Name-Age pair: " + nameAge);
        System.out.println("Coordinates pair: " + coordinates);
        System.out.println("Flag-Message pair: " + flagMessage);
        
        // Swap pairs
        GenericPair<Integer, String> swappedNameAge = nameAge.swap();
        System.out.println("Swapped name-age: " + swappedNameAge);
        
        // Map transformations
        GenericPair<String, String> uppercaseLength = nameAge.map(
            String::toUpperCase,
            age -> "Age: " + age
        );
        System.out.println("Transformed pair: " + uppercaseLength);
        
        // Coordinate transformations
        GenericPair<Integer, Integer> roundedCoords = coordinates.map(
            Math::round,
            Math::round
        );
        System.out.println("Rounded coordinates: " + roundedCoords);
        
        // Collection of pairs
        List<GenericPair<String, Double>> studentGrades = Arrays.asList(
            GenericPair.of("Alice", 95.5),
            GenericPair.of("Bob", 87.2),
            GenericPair.of("Carol", 92.8),
            GenericPair.of("David", 78.9)
        );
        
        System.out.println("Student grades:");
        studentGrades.forEach(pair -> 
            System.out.printf("  %s: %.1f%n", pair.getFirst(), pair.getSecond()));
        
        // Find best student
        GenericPair<String, Double> bestStudent = studentGrades.stream()
            .max(Comparator.comparing(GenericPair::getSecond))
            .orElse(null);
        
        if (bestStudent != null) {
            System.out.println("Best student: " + bestStudent.getFirst() + 
                             " with grade " + bestStudent.getSecond());
        }
        
        System.out.println();
    }
}
```

---

## 🧪 Part 5: Generic Utility Framework (30 minutes)

### **Complete Generic Utility Framework**

Create `GenericUtilityFramework.java`:

```java
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

/**
 * Complete Generic Utility Framework
 * Demonstrates advanced generic patterns and practical utilities
 */
public class GenericUtilityFramework {
    
    // Generic Result class for handling success/failure scenarios
    static class Result<T, E> {
        private final T value;
        private final E error;
        private final boolean isSuccess;
        
        private Result(T value, E error, boolean isSuccess) {
            this.value = value;
            this.error = error;
            this.isSuccess = isSuccess;
        }
        
        public static <T, E> Result<T, E> success(T value) {
            return new Result<>(value, null, true);
        }
        
        public static <T, E> Result<T, E> failure(E error) {
            return new Result<>(null, error, false);
        }
        
        public boolean isSuccess() {
            return isSuccess;
        }
        
        public boolean isFailure() {
            return !isSuccess;
        }
        
        public T getValue() {
            if (!isSuccess) {
                throw new IllegalStateException("Cannot get value from failed result");
            }
            return value;
        }
        
        public E getError() {
            if (isSuccess) {
                throw new IllegalStateException("Cannot get error from successful result");
            }
            return error;
        }
        
        public T getValueOrDefault(T defaultValue) {
            return isSuccess ? value : defaultValue;
        }
        
        public <U> Result<U, E> map(Function<T, U> mapper) {
            if (isSuccess) {
                return Result.success(mapper.apply(value));
            } else {
                return Result.failure(error);
            }
        }
        
        public <F> Result<T, F> mapError(Function<E, F> errorMapper) {
            if (isFailure()) {
                return Result.failure(errorMapper.apply(error));
            } else {
                return Result.success(value);
            }
        }
        
        @Override
        public String toString() {
            if (isSuccess) {
                return "Success[" + value + "]";
            } else {
                return "Failure[" + error + "]";
            }
        }
    }
    
    // Generic Optional-like class with additional utilities
    static class Optional<T> {
        private final T value;
        
        private Optional(T value) {
            this.value = value;
        }
        
        public static <T> Optional<T> of(T value) {
            if (value == null) {
                throw new IllegalArgumentException("Value cannot be null");
            }
            return new Optional<>(value);
        }
        
        public static <T> Optional<T> ofNullable(T value) {
            return new Optional<>(value);
        }
        
        public static <T> Optional<T> empty() {
            return new Optional<>(null);
        }
        
        public boolean isPresent() {
            return value != null;
        }
        
        public boolean isEmpty() {
            return value == null;
        }
        
        public T get() {
            if (value == null) {
                throw new IllegalStateException("No value present");
            }
            return value;
        }
        
        public T orElse(T other) {
            return value != null ? value : other;
        }
        
        public T orElseGet(Supplier<T> supplier) {
            return value != null ? value : supplier.get();
        }
        
        public <U> Optional<U> map(Function<T, U> mapper) {
            if (value == null) {
                return empty();
            }
            return Optional.ofNullable(mapper.apply(value));
        }
        
        public <U> Optional<U> flatMap(Function<T, Optional<U>> mapper) {
            if (value == null) {
                return empty();
            }
            return mapper.apply(value);
        }
        
        public Optional<T> filter(Predicate<T> predicate) {
            if (value == null || !predicate.test(value)) {
                return empty();
            }
            return this;
        }
        
        public void ifPresent(Consumer<T> action) {
            if (value != null) {
                action.accept(value);
            }
        }
        
        @Override
        public String toString() {
            return value != null ? "Optional[" + value + "]" : "Optional.empty";
        }
    }
    
    // Generic Utility Methods
    static class GenericUtils {
        
        // Safe array access
        public static <T> Optional<T> safeGet(T[] array, int index) {
            if (array == null || index < 0 || index >= array.length) {
                return Optional.empty();
            }
            return Optional.ofNullable(array[index]);
        }
        
        // Safe list access
        public static <T> Optional<T> safeGet(List<T> list, int index) {
            if (list == null || index < 0 || index >= list.size()) {
                return Optional.empty();
            }
            return Optional.ofNullable(list.get(index));
        }
        
        // Safe map access
        public static <K, V> Optional<V> safeGet(Map<K, V> map, K key) {
            if (map == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(map.get(key));
        }
        
        // Combine two optionals
        public static <T, U, R> Optional<R> combine(Optional<T> opt1, Optional<U> opt2, 
                                                   BinaryOperator<R> combiner) {
            if (opt1.isPresent() && opt2.isPresent()) {
                // This requires both values to be of type R, which is a simplification
                // In practice, you'd use a BiFunction<T, U, R>
                return Optional.of(combiner.apply((R)opt1.get(), (R)opt2.get()));
            }
            return Optional.empty();
        }
        
        // Filter and transform collection
        public static <T, R> List<R> filterAndMap(Collection<T> collection, 
                                                 Predicate<T> filter, 
                                                 Function<T, R> mapper) {
            return collection.stream()
                           .filter(filter)
                           .map(mapper)
                           .collect(Collectors.toList());
        }
        
        // Group by key
        public static <T, K> Map<K, List<T>> groupBy(Collection<T> collection, 
                                                    Function<T, K> keyExtractor) {
            return collection.stream()
                           .collect(Collectors.groupingBy(keyExtractor));
        }
        
        // Partition collection
        public static <T> Map<Boolean, List<T>> partition(Collection<T> collection, 
                                                         Predicate<T> predicate) {
            return collection.stream()
                           .collect(Collectors.partitioningBy(predicate));
        }
        
        // Find first match
        public static <T> Optional<T> findFirst(Collection<T> collection, 
                                              Predicate<T> predicate) {
            return collection.stream()
                           .filter(predicate)
                           .findFirst()
                           .map(Optional::of)
                           .orElse(Optional.empty());
        }
        
        // Safe operation with Result
        public static <T, R> Result<R, Exception> safeOperation(T input, 
                                                              Function<T, R> operation) {
            try {
                R result = operation.apply(input);
                return Result.success(result);
            } catch (Exception e) {
                return Result.failure(e);
            }
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Generic Utility Framework Demonstration ===\n");
        
        // Result demonstrations
        demonstrateResult();
        
        // Optional demonstrations
        demonstrateOptional();
        
        // Utility methods demonstrations
        demonstrateUtilityMethods();
        
        // Real-world examples
        demonstrateRealWorldExamples();
    }
    
    private static void demonstrateResult() {
        System.out.println("1. Result<T, E> for Error Handling:");
        
        // Successful operations
        Result<Integer, String> successResult = Result.success(42);
        Result<String, String> stringResult = Result.success("Hello, World!");
        
        System.out.println("Success result: " + successResult);
        System.out.println("String result: " + stringResult);
        
        // Failed operations
        Result<Integer, String> failureResult = Result.failure("Division by zero");
        Result<String, Exception> exceptionResult = Result.failure(new IllegalArgumentException("Invalid input"));
        
        System.out.println("Failure result: " + failureResult);
        System.out.println("Exception result: " + exceptionResult);
        
        // Using results
        if (successResult.isSuccess()) {
            System.out.println("Success value: " + successResult.getValue());
        }
        
        if (failureResult.isFailure()) {
            System.out.println("Failure error: " + failureResult.getError());
        }
        
        // Mapping results
        Result<String, String> mappedResult = successResult.map(num -> "Number: " + num);
        System.out.println("Mapped result: " + mappedResult);
        
        Result<String, String> mappedFailure = failureResult.map(num -> "Number: " + num);
        System.out.println("Mapped failure: " + mappedFailure);
        
        System.out.println();
    }
    
    private static void demonstrateOptional() {
        System.out.println("2. Optional<T> for Null Safety:");
        
        // Creating optionals
        Optional<String> presentOptional = Optional.of("Present value");
        Optional<String> emptyOptional = Optional.empty();
        Optional<String> nullableOptional = Optional.ofNullable(null);
        
        System.out.println("Present optional: " + presentOptional);
        System.out.println("Empty optional: " + emptyOptional);
        System.out.println("Nullable optional: " + nullableOptional);
        
        // Using optionals
        System.out.println("Is present: " + presentOptional.isPresent());
        System.out.println("Is empty: " + emptyOptional.isEmpty());
        
        // Safe access
        String value1 = presentOptional.orElse("Default");
        String value2 = emptyOptional.orElse("Default");
        System.out.println("Value 1: " + value1);
        System.out.println("Value 2: " + value2);
        
        // Mapping
        Optional<Integer> length = presentOptional.map(String::length);
        System.out.println("Mapped length: " + length);
        
        Optional<Integer> emptyLength = emptyOptional.map(String::length);
        System.out.println("Empty mapped length: " + emptyLength);
        
        // Filtering
        Optional<String> filtered = presentOptional.filter(s -> s.length() > 5);
        System.out.println("Filtered (length > 5): " + filtered);
        
        // Action if present
        presentOptional.ifPresent(s -> System.out.println("Action performed on: " + s));
        emptyOptional.ifPresent(s -> System.out.println("This won't be printed"));
        
        System.out.println();
    }
    
    private static void demonstrateUtilityMethods() {
        System.out.println("3. Generic Utility Methods:");
        
        // Safe array access
        String[] array = {"a", "b", "c", "d", "e"};
        System.out.println("Safe array access:");
        System.out.println("  Index 2: " + GenericUtils.safeGet(array, 2));
        System.out.println("  Index 10: " + GenericUtils.safeGet(array, 10));
        System.out.println("  Index -1: " + GenericUtils.safeGet(array, -1));
        
        // Safe list access
        List<Integer> numbers = Arrays.asList(10, 20, 30, 40, 50);
        System.out.println("Safe list access:");
        System.out.println("  Index 1: " + GenericUtils.safeGet(numbers, 1));
        System.out.println("  Index 100: " + GenericUtils.safeGet(numbers, 100));
        
        // Safe map access
        Map<String, String> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");
        
        System.out.println("Safe map access:");
        System.out.println("  key1: " + GenericUtils.safeGet(map, "key1"));
        System.out.println("  key3: " + GenericUtils.safeGet(map, "key3"));
        
        // Filter and map
        List<String> words = Arrays.asList("java", "python", "javascript", "go", "rust");
        List<Integer> lengths = GenericUtils.filterAndMap(
            words,
            word -> word.length() > 4,
            String::length
        );
        System.out.println("Words longer than 4 characters (lengths): " + lengths);
        
        // Group by
        Map<Integer, List<String>> groupedByLength = GenericUtils.groupBy(words, String::length);
        System.out.println("Words grouped by length: " + groupedByLength);
        
        // Partition
        Map<Boolean, List<String>> partitioned = GenericUtils.partition(words, w -> w.startsWith("j"));
        System.out.println("Words starting with 'j': " + partitioned.get(true));
        System.out.println("Other words: " + partitioned.get(false));
        
        System.out.println();
    }
    
    private static void demonstrateRealWorldExamples() {
        System.out.println("4. Real-World Examples:");
        
        // Safe string to integer conversion
        Result<Integer, Exception> parseResult1 = GenericUtils.safeOperation("123", Integer::parseInt);
        Result<Integer, Exception> parseResult2 = GenericUtils.safeOperation("abc", Integer::parseInt);
        
        System.out.println("Parse '123': " + parseResult1);
        System.out.println("Parse 'abc': " + parseResult2);
        
        // Safe division
        Result<Double, Exception> divisionResult1 = GenericUtils.safeOperation(10.0, x -> x / 2.0);
        Result<Double, Exception> divisionResult2 = GenericUtils.safeOperation(10.0, x -> x / 0.0);
        
        System.out.println("Division 10.0 / 2.0: " + divisionResult1);
        System.out.println("Division 10.0 / 0.0: " + divisionResult2);
        
        // Chaining optional operations
        Optional<String> input = Optional.of("   Hello World   ");
        Optional<String> processed = input
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(String::toUpperCase)
            .filter(s -> s.length() < 20);
        
        System.out.println("Processed input: " + processed);
        
        // Finding first match with custom predicate
        List<String> programming_languages = Arrays.asList(
            "Java", "Python", "JavaScript", "C++", "Go", "Rust", "Kotlin"
        );
        
        Optional<String> firstLongName = GenericUtils.findFirst(
            programming_languages, 
            name -> name.length() > 6
        );
        System.out.println("First language with >6 characters: " + firstLongName);
        
        Optional<String> firstWithJ = GenericUtils.findFirst(
            programming_languages,
            name -> name.startsWith("J")
        );
        System.out.println("First language starting with 'J': " + firstWithJ);
        
        System.out.println();
    }
}
```

---

## 📝 Key Takeaways from Day 12

### **Generic Classes and Interfaces:**
- ✅ **Type Parameters:** Use `<T>`, `<T, U>` for flexible class definitions
- ✅ **Type Safety:** Compile-time checking prevents ClassCastException
- ✅ **Code Reuse:** Write once, use with multiple types
- ✅ **Collections:** Generic collections eliminate casting

### **Bounded Type Parameters:**
- ✅ **Upper Bounds:** `<T extends Type>` restricts to subtypes
- ✅ **Multiple Bounds:** `<T extends Class & Interface>` for multiple constraints
- ✅ **Comparable Bounds:** Enable sorting and comparison operations
- ✅ **Number Bounds:** Allow arithmetic operations on generic types

### **Wildcards:**
- ✅ **Upper Bounded:** `? extends Type` for reading (Producer Extends)
- ✅ **Lower Bounded:** `? super Type` for writing (Consumer Super)
- ✅ **Unbounded:** `?` for unknown types
- ✅ **PECS Principle:** Producer Extends, Consumer Super guideline

### **Advanced Patterns:**
- ✅ **Generic Methods:** Independent of class type parameters
- ✅ **Type Erasure:** Understanding runtime type information loss
- ✅ **Wildcard Capture:** Helper methods to capture wildcard types
- ✅ **Utility Classes:** Building reusable generic utility libraries

---

## 🎯 Today's Success Criteria

**Completed Tasks:**
- [ ] Create generic classes with single and multiple type parameters
- [ ] Implement bounded type parameters for constrained generics
- [ ] Use wildcards correctly with PECS principle
- [ ] Build custom generic data structures (Stack, Queue, BST)
- [ ] Develop generic utility framework with Result and Optional patterns
- [ ] Understand type erasure and its implications

**Bonus Achievements:**
- [ ] Implement generic algorithms for searching and sorting
- [ ] Create functional-style generic utilities
- [ ] Build type-safe builders and factory patterns
- [ ] Experiment with recursive generic types
- [ ] Design generic event handling systems

---

## 🚀 Tomorrow's Preview (Day 13 - July 22)

**Focus:** File I/O and Serialization
- File and directory operations
- Reading and writing text files
- Binary file operations
- Object serialization and deserialization
- Modern NIO.2 file operations

---

## 🎉 Exceptional Progress!

You've mastered one of Java's most powerful features! Generics enable type-safe, reusable code that forms the backbone of modern Java applications. Tomorrow we'll explore File I/O for persistent data storage.

**Keep building type-safe applications!** 🚀