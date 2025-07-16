# Day 11: Collections Framework - Part 2 (July 20, 2025)

**Date:** July 20, 2025  
**Duration:** 2.5 hours  
**Focus:** Master Maps, Queues, and Custom Sorting for advanced data management

---

## 🎯 Today's Learning Goals

By the end of this session, you'll:
- ✅ Master Map interface (HashMap, TreeMap, LinkedHashMap)
- ✅ Understand Queue and Deque interfaces with practical examples
- ✅ Learn Comparable and Comparator for custom sorting
- ✅ Build Phone Book Application using Maps
- ✅ Create Task Priority Management System with Queues
- ✅ Implement custom sorting for complex objects

---

## 🗺️ Part 1: Map Interface (45 minutes)

### **Understanding Maps**

**Map Interface**: A collection that maps keys to values, where each key can map to at most one value.

**Key Characteristics:**
- No duplicate keys allowed
- Each key maps to exactly one value
- Not part of Collection hierarchy (separate interface)
- Provides key-value pair storage

### **Map Implementations Comparison**

| Feature | HashMap | LinkedHashMap | TreeMap |
|---------|---------|---------------|---------|
| **Ordering** | None | Insertion order | Sorted by keys |
| **Performance** | O(1) | O(1) | O(log n) |
| **Null Keys** | One null key | One null key | No null keys |
| **Use Case** | Fast lookup | Ordered fast lookup | Sorted collection |

### **HashMap Examples**

Create `HashMapDemo.java`:

```java
import java.util.*;

/**
 * HashMap demonstration showing key-value operations
 * Best for: Fast lookups, no ordering needed
 */
public class HashMapDemo {
    
    public static void main(String[] args) {
        System.out.println("=== HashMap Demonstration ===\n");
        
        // Basic HashMap operations
        demonstrateBasicOperations();
        
        // Iteration patterns
        demonstrateIteration();
        
        // Practical examples
        demonstratePracticalExamples();
    }
    
    private static void demonstrateBasicOperations() {
        System.out.println("1. Basic HashMap Operations:");
        
        // Creating and populating HashMap
        Map<String, Integer> studentGrades = new HashMap<>();
        
        // Adding key-value pairs
        studentGrades.put("Alice", 95);
        studentGrades.put("Bob", 87);
        studentGrades.put("Carol", 92);
        studentGrades.put("David", 78);
        studentGrades.put("Alice", 97); // Updates existing value
        
        System.out.println("Student grades: " + studentGrades);
        System.out.println("Size: " + studentGrades.size());
        
        // Accessing values
        System.out.println("Alice's grade: " + studentGrades.get("Alice"));
        System.out.println("Eve's grade: " + studentGrades.get("Eve")); // null for non-existent key
        
        // Safe access with default value
        System.out.println("Eve's grade (with default): " + 
                         studentGrades.getOrDefault("Eve", 0));
        
        // Checking existence
        System.out.println("Contains key 'Bob': " + studentGrades.containsKey("Bob"));
        System.out.println("Contains value 92: " + studentGrades.containsValue(92));
        
        // Removing entries
        Integer removedGrade = studentGrades.remove("David");
        System.out.println("Removed David's grade: " + removedGrade);
        System.out.println("After removal: " + studentGrades);
        
        // Advanced operations
        studentGrades.putIfAbsent("Eve", 85); // Only adds if key doesn't exist
        studentGrades.replace("Bob", 87, 90); // Replace only if current value matches
        
        System.out.println("Final grades: " + studentGrades);
        System.out.println();
    }
    
    private static void demonstrateIteration() {
        System.out.println("2. HashMap Iteration Patterns:");
        
        Map<String, String> countryCapitals = new HashMap<>();
        countryCapitals.put("USA", "Washington D.C.");
        countryCapitals.put("France", "Paris");
        countryCapitals.put("Japan", "Tokyo");
        countryCapitals.put("Germany", "Berlin");
        
        // Iterate over keys
        System.out.println("Countries:");
        for (String country : countryCapitals.keySet()) {
            System.out.println("  " + country);
        }
        
        // Iterate over values
        System.out.println("Capitals:");
        for (String capital : countryCapitals.values()) {
            System.out.println("  " + capital);
        }
        
        // Iterate over key-value pairs (recommended)
        System.out.println("Country-Capital pairs:");
        for (Map.Entry<String, String> entry : countryCapitals.entrySet()) {
            System.out.println("  " + entry.getKey() + " -> " + entry.getValue());
        }
        
        // Modern forEach approach (Java 8+)
        System.out.println("Using forEach:");
        countryCapitals.forEach((country, capital) -> 
            System.out.println("  " + country + " has capital " + capital));
        
        System.out.println();
    }
    
    private static void demonstratePracticalExamples() {
        System.out.println("3. Practical HashMap Examples:");
        
        // Example 1: Word frequency counter
        demonstrateWordFrequency();
        
        // Example 2: Inventory management
        demonstrateInventoryManagement();
    }
    
    private static void demonstrateWordFrequency() {
        System.out.println("Example 1: Word Frequency Counter");
        
        String text = "java is great and java is powerful and java is fun";
        String[] words = text.split(" ");
        
        Map<String, Integer> wordCount = new HashMap<>();
        
        for (String word : words) {
            wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
        }
        
        System.out.println("Word frequencies:");
        for (Map.Entry<String, Integer> entry : wordCount.entrySet()) {
            System.out.println("  '" + entry.getKey() + "': " + entry.getValue());
        }
        System.out.println();
    }
    
    private static void demonstrateInventoryManagement() {
        System.out.println("Example 2: Simple Inventory Management");
        
        Map<String, Integer> inventory = new HashMap<>();
        
        // Adding items
        inventory.put("Laptop", 15);
        inventory.put("Mouse", 50);
        inventory.put("Keyboard", 30);
        inventory.put("Monitor", 20);
        
        System.out.println("Initial inventory: " + inventory);
        
        // Simulating sales and restocking
        String item = "Laptop";
        int sold = 3;
        if (inventory.containsKey(item) && inventory.get(item) >= sold) {
            inventory.put(item, inventory.get(item) - sold);
            System.out.println("Sold " + sold + " " + item + "s");
        }
        
        // Restocking
        inventory.put("Mouse", inventory.get("Mouse") + 25);
        System.out.println("Restocked 25 mice");
        
        System.out.println("Updated inventory: " + inventory);
        System.out.println();
    }
}
```

### **TreeMap and LinkedHashMap Examples**

Create `MapComparisonDemo.java`:

```java
import java.util.*;

/**
 * Comparison of different Map implementations
 * Shows ordering differences and performance characteristics
 */
public class MapComparisonDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Map Implementation Comparison ===\n");
        
        // Compare ordering behavior
        demonstrateOrderingDifferences();
        
        // TreeMap specific features
        demonstrateTreeMapFeatures();
        
        // LinkedHashMap features
        demonstrateLinkedHashMapFeatures();
    }
    
    private static void demonstrateOrderingDifferences() {
        System.out.println("1. Ordering Differences:");
        
        // Same data, different maps
        String[] keys = {"Charlie", "Alice", "Bob", "David"};
        
        // HashMap - no guaranteed order
        Map<String, Integer> hashMap = new HashMap<>();
        for (int i = 0; i < keys.length; i++) {
            hashMap.put(keys[i], i + 1);
        }
        
        // LinkedHashMap - insertion order
        Map<String, Integer> linkedHashMap = new LinkedHashMap<>();
        for (int i = 0; i < keys.length; i++) {
            linkedHashMap.put(keys[i], i + 1);
        }
        
        // TreeMap - sorted order
        Map<String, Integer> treeMap = new TreeMap<>();
        for (int i = 0; i < keys.length; i++) {
            treeMap.put(keys[i], i + 1);
        }
        
        System.out.println("HashMap (no order): " + hashMap);
        System.out.println("LinkedHashMap (insertion order): " + linkedHashMap);
        System.out.println("TreeMap (sorted): " + treeMap);
        System.out.println();
    }
    
    private static void demonstrateTreeMapFeatures() {
        System.out.println("2. TreeMap Specific Features:");
        
        TreeMap<String, Double> scores = new TreeMap<>();
        scores.put("Mathematics", 95.5);
        scores.put("Physics", 88.0);
        scores.put("Chemistry", 92.3);
        scores.put("Biology", 89.7);
        scores.put("English", 91.2);
        
        System.out.println("All scores (sorted by subject): " + scores);
        
        // TreeMap navigation methods
        System.out.println("First subject: " + scores.firstKey());
        System.out.println("Last subject: " + scores.lastKey());
        System.out.println("Subject before Mathematics: " + scores.lowerKey("Mathematics"));
        System.out.println("Subject after Mathematics: " + scores.higherKey("Mathematics"));
        
        // Range operations
        SortedMap<String, Double> subMap = scores.subMap("Chemistry", "Physics");
        System.out.println("Subjects from Chemistry to Physics: " + subMap);
        
        // Head and tail maps
        SortedMap<String, Double> headMap = scores.headMap("Mathematics");
        System.out.println("Subjects before Mathematics: " + headMap);
        
        SortedMap<String, Double> tailMap = scores.tailMap("Mathematics");
        System.out.println("Subjects from Mathematics onwards: " + tailMap);
        
        System.out.println();
    }
    
    private static void demonstrateLinkedHashMapFeatures() {
        System.out.println("3. LinkedHashMap Features:");
        
        // Access-order LinkedHashMap (LRU behavior)
        Map<String, String> lruCache = new LinkedHashMap<String, String>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                return size() > 3; // Keep only 3 entries
            }
        };
        
        // Adding entries
        lruCache.put("file1.txt", "Content 1");
        lruCache.put("file2.txt", "Content 2");
        lruCache.put("file3.txt", "Content 3");
        
        System.out.println("Initial cache: " + lruCache);
        
        // Access file1 (moves it to end)
        lruCache.get("file1.txt");
        System.out.println("After accessing file1: " + lruCache);
        
        // Add new file (should remove least recently used)
        lruCache.put("file4.txt", "Content 4");
        System.out.println("After adding file4: " + lruCache);
        
        // Regular LinkedHashMap (insertion order)
        Map<String, Integer> insertionOrder = new LinkedHashMap<>();
        insertionOrder.put("Third", 3);
        insertionOrder.put("First", 1);
        insertionOrder.put("Second", 2);
        
        System.out.println("Insertion order preserved: " + insertionOrder);
        System.out.println();
    }
}
```

---

## 📋 Part 2: Queue and Deque Interfaces (45 minutes)

### **Understanding Queues**

**Queue Interface**: A collection designed for holding elements prior to processing, typically in FIFO (First-In-First-Out) order.

**Common Operations:**
- `offer(e)` / `add(e)` - Add element to queue
- `poll()` / `remove()` - Remove and return head element
- `peek()` / `element()` - Return head element without removing

### **Queue Implementations**

Create `QueueDemo.java`:

```java
import java.util.*;

/**
 * Queue interface demonstration with different implementations
 * Shows FIFO operations and various queue types
 */
public class QueueDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Queue Interface Demonstration ===\n");
        
        // Basic queue operations
        demonstrateBasicQueueOperations();
        
        // Priority queue
        demonstratePriorityQueue();
        
        // Deque operations
        demonstrateDequeOperations();
        
        // Real-world examples
        demonstrateRealWorldExamples();
    }
    
    private static void demonstrateBasicQueueOperations() {
        System.out.println("1. Basic Queue Operations (LinkedList as Queue):");
        
        Queue<String> customerQueue = new LinkedList<>();
        
        // Adding customers to queue
        customerQueue.offer("Alice");
        customerQueue.offer("Bob");
        customerQueue.offer("Carol");
        customerQueue.offer("David");
        
        System.out.println("Initial queue: " + customerQueue);
        System.out.println("Queue size: " + customerQueue.size());
        
        // Peek at front customer
        System.out.println("Next customer (peek): " + customerQueue.peek());
        System.out.println("Queue after peek: " + customerQueue);
        
        // Serve customers (FIFO)
        System.out.println("\nServing customers:");
        while (!customerQueue.isEmpty()) {
            String customer = customerQueue.poll();
            System.out.println("  Serving: " + customer);
            System.out.println("  Remaining: " + customerQueue);
        }
        
        // Safe operations on empty queue
        System.out.println("Peek on empty queue: " + customerQueue.peek()); // null
        System.out.println("Poll on empty queue: " + customerQueue.poll()); // null
        
        System.out.println();
    }
    
    private static void demonstratePriorityQueue() {
        System.out.println("2. PriorityQueue Operations:");
        
        // Natural ordering (integers)
        PriorityQueue<Integer> numbers = new PriorityQueue<>();
        numbers.offer(15);
        numbers.offer(10);
        numbers.offer(20);
        numbers.offer(8);
        numbers.offer(25);
        
        System.out.println("PriorityQueue (natural order): " + numbers);
        
        System.out.println("Processing in priority order:");
        while (!numbers.isEmpty()) {
            System.out.println("  Priority: " + numbers.poll());
        }
        
        // Custom ordering (reverse order)
        PriorityQueue<String> tasks = new PriorityQueue<>(Collections.reverseOrder());
        tasks.offer("Low Priority Task");
        tasks.offer("High Priority Task");
        tasks.offer("Medium Priority Task");
        tasks.offer("Critical Task");
        
        System.out.println("\nTask queue (reverse alphabetical):");
        while (!tasks.isEmpty()) {
            System.out.println("  Next task: " + tasks.poll());
        }
        
        System.out.println();
    }
    
    private static void demonstrateDequeOperations() {
        System.out.println("3. Deque Operations (Double-ended Queue):");
        
        Deque<String> deque = new ArrayDeque<>();
        
        // Add to both ends
        deque.addFirst("Middle");
        deque.addFirst("First");
        deque.addLast("Last");
        deque.addLast("End");
        
        System.out.println("After adding to both ends: " + deque);
        
        // Peek at both ends
        System.out.println("First element: " + deque.peekFirst());
        System.out.println("Last element: " + deque.peekLast());
        
        // Remove from both ends
        System.out.println("Removed from front: " + deque.removeFirst());
        System.out.println("Removed from back: " + deque.removeLast());
        System.out.println("After removals: " + deque);
        
        // Use as stack (LIFO)
        System.out.println("\nUsing Deque as Stack:");
        Deque<String> stack = new ArrayDeque<>();
        stack.push("Bottom");
        stack.push("Middle");
        stack.push("Top");
        
        System.out.println("Stack: " + stack);
        while (!stack.isEmpty()) {
            System.out.println("  Popped: " + stack.pop());
        }
        
        System.out.println();
    }
    
    private static void demonstrateRealWorldExamples() {
        System.out.println("4. Real-World Queue Examples:");
        
        // Example 1: Print job queue
        demonstratePrintJobQueue();
        
        // Example 2: Web browser history
        demonstrateBrowserHistory();
    }
    
    private static void demonstratePrintJobQueue() {
        System.out.println("Example 1: Print Job Management");
        
        Queue<String> printQueue = new PriorityQueue<>();
        
        // Add print jobs (will be sorted alphabetically)
        printQueue.offer("Report.pdf");
        printQueue.offer("Image.jpg");
        printQueue.offer("Document.docx");
        printQueue.offer("Spreadsheet.xlsx");
        
        System.out.println("Print jobs in queue: " + printQueue.size());
        
        System.out.println("Processing print jobs:");
        while (!printQueue.isEmpty()) {
            String job = printQueue.poll();
            System.out.println("  Printing: " + job);
            // Simulate printing time
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("All print jobs completed!");
        System.out.println();
    }
    
    private static void demonstrateBrowserHistory() {
        System.out.println("Example 2: Browser History (using Deque)");
        
        Deque<String> browserHistory = new ArrayDeque<>();
        
        // Navigate to pages
        browserHistory.addLast("google.com");
        browserHistory.addLast("github.com");
        browserHistory.addLast("stackoverflow.com");
        browserHistory.addLast("oracle.com/java");
        
        System.out.println("Current history: " + browserHistory);
        System.out.println("Current page: " + browserHistory.peekLast());
        
        // Go back
        String previousPage = browserHistory.removeLast();
        System.out.println("Going back from: " + previousPage);
        System.out.println("Now on: " + browserHistory.peekLast());
        
        // Go forward (add new page)
        browserHistory.addLast("docs.oracle.com");
        System.out.println("Navigated to: " + browserHistory.peekLast());
        System.out.println("Final history: " + browserHistory);
        System.out.println();
    }
}
```

---

## 🎯 Part 3: Comparable and Comparator (45 minutes)

### **Custom Sorting Concepts**

**Comparable Interface**: Defines natural ordering for a class
**Comparator Interface**: Defines custom ordering separate from the class

### **Person Class with Comparable**

Create `Person.java`:

```java
import java.util.*;

/**
 * Person class implementing Comparable for natural ordering
 * Demonstrates natural sorting by age
 */
public class Person implements Comparable<Person> {
    private String name;
    private int age;
    private String email;
    private double salary;
    
    public Person(String name, int age, String email, double salary) {
        this.name = name;
        this.age = age;
        this.email = email;
        this.salary = salary;
    }
    
    // Natural ordering by age (Comparable)
    @Override
    public int compareTo(Person other) {
        return Integer.compare(this.age, other.age);
    }
    
    // Getters
    public String getName() { return name; }
    public int getAge() { return age; }
    public String getEmail() { return email; }
    public double getSalary() { return salary; }
    
    @Override
    public String toString() {
        return String.format("Person[name='%s', age=%d, email='%s', salary=%.2f]", 
                           name, age, email, salary);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Person person = (Person) obj;
        return age == person.age && 
               Objects.equals(name, person.name) && 
               Objects.equals(email, person.email);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, age, email);
    }
}
```

### **Comparator Examples**

Create `SortingDemo.java`:

```java
import java.util.*;

/**
 * Comprehensive sorting demonstration with Comparable and Comparator
 * Shows various sorting strategies for custom objects
 */
public class SortingDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Custom Sorting Demonstration ===\n");
        
        // Create sample data
        List<Person> people = createSamplePeople();
        
        // Natural ordering (Comparable)
        demonstrateNaturalOrdering(people);
        
        // Custom comparators
        demonstrateCustomComparators(people);
        
        // Multiple field sorting
        demonstrateMultipleFieldSorting(people);
        
        // Sorting with Collections.sort vs Stream API
        demonstrateSortingMethods(people);
    }
    
    private static List<Person> createSamplePeople() {
        List<Person> people = new ArrayList<>();
        people.add(new Person("Alice Johnson", 28, "alice@email.com", 75000));
        people.add(new Person("Bob Smith", 35, "bob@email.com", 68000));
        people.add(new Person("Carol Davis", 22, "carol@email.com", 52000));
        people.add(new Person("David Wilson", 45, "david@email.com", 89000));
        people.add(new Person("Eve Brown", 31, "eve@email.com", 71000));
        people.add(new Person("Frank Miller", 28, "frank@email.com", 63000));
        
        return people;
    }
    
    private static void demonstrateNaturalOrdering(List<Person> people) {
        System.out.println("1. Natural Ordering (Comparable - by age):");
        
        List<Person> sortedByAge = new ArrayList<>(people);
        Collections.sort(sortedByAge);
        
        System.out.println("People sorted by age (natural ordering):");
        for (Person person : sortedByAge) {
            System.out.println("  " + person);
        }
        System.out.println();
    }
    
    private static void demonstrateCustomComparators(List<Person> people) {
        System.out.println("2. Custom Comparators:");
        
        // Sort by name
        List<Person> sortedByName = new ArrayList<>(people);
        Collections.sort(sortedByName, new Comparator<Person>() {
            @Override
            public int compare(Person p1, Person p2) {
                return p1.getName().compareTo(p2.getName());
            }
        });
        
        System.out.println("Sorted by name:");
        sortedByName.forEach(p -> System.out.println("  " + p));
        
        // Sort by salary (descending) using lambda
        List<Person> sortedBySalary = new ArrayList<>(people);
        sortedBySalary.sort((p1, p2) -> Double.compare(p2.getSalary(), p1.getSalary()));
        
        System.out.println("\nSorted by salary (highest first):");
        sortedBySalary.forEach(p -> System.out.println("  " + p));
        
        // Sort by email domain
        List<Person> sortedByEmailDomain = new ArrayList<>(people);
        sortedByEmailDomain.sort(Comparator.comparing(p -> p.getEmail().split("@")[1]));
        
        System.out.println("\nSorted by email domain:");
        sortedByEmailDomain.forEach(p -> System.out.println("  " + p));
        
        System.out.println();
    }
    
    private static void demonstrateMultipleFieldSorting(List<Person> people) {
        System.out.println("3. Multiple Field Sorting:");
        
        // Sort by age first, then by name
        List<Person> multiSort1 = new ArrayList<>(people);
        multiSort1.sort(Comparator.comparing(Person::getAge)
                                  .thenComparing(Person::getName));
        
        System.out.println("Sorted by age, then by name:");
        multiSort1.forEach(p -> System.out.println("  " + p));
        
        // Sort by salary (desc), then by age (asc), then by name (asc)
        List<Person> multiSort2 = new ArrayList<>(people);
        multiSort2.sort(Comparator.comparing(Person::getSalary).reversed()
                                  .thenComparing(Person::getAge)
                                  .thenComparing(Person::getName));
        
        System.out.println("\nSorted by salary (desc), age (asc), name (asc):");
        multiSort2.forEach(p -> System.out.println("  " + p));
        
        System.out.println();
    }
    
    private static void demonstrateSortingMethods(List<Person> people) {
        System.out.println("4. Different Sorting Methods:");
        
        // Method 1: Collections.sort()
        List<Person> method1 = new ArrayList<>(people);
        Collections.sort(method1, Comparator.comparing(Person::getName));
        System.out.println("Method 1 - Collections.sort():");
        method1.forEach(p -> System.out.println("  " + p.getName()));
        
        // Method 2: List.sort()
        List<Person> method2 = new ArrayList<>(people);
        method2.sort(Comparator.comparing(Person::getName));
        System.out.println("\nMethod 2 - List.sort():");
        method2.forEach(p -> System.out.println("  " + p.getName()));
        
        // Method 3: Stream API (creates new list)
        System.out.println("\nMethod 3 - Stream API:");
        people.stream()
              .sorted(Comparator.comparing(Person::getName))
              .forEach(p -> System.out.println("  " + p.getName()));
        
        // Method 4: TreeSet (automatically sorted)
        System.out.println("\nMethod 4 - TreeSet (auto-sorted by name):");
        Set<Person> treeSet = new TreeSet<>(Comparator.comparing(Person::getName));
        treeSet.addAll(people);
        treeSet.forEach(p -> System.out.println("  " + p.getName()));
        
        System.out.println();
    }
}
```

---

## 📱 Part 4: Phone Book Application (30 minutes)

### **Complete Phone Book System**

Create `PhoneBookApplication.java`:

```java
import java.util.*;

/**
 * Complete Phone Book Application using Maps
 * Demonstrates practical use of HashMap, TreeMap, and advanced Map operations
 */
public class PhoneBookApplication {
    
    // Contact class for storing contact information
    static class Contact {
        private String name;
        private String phoneNumber;
        private String email;
        private String address;
        private String category; // Family, Work, Friend, etc.
        
        public Contact(String name, String phoneNumber, String email, String address, String category) {
            this.name = name;
            this.phoneNumber = phoneNumber;
            this.email = email;
            this.address = address;
            this.category = category;
        }
        
        // Getters
        public String getName() { return name; }
        public String getPhoneNumber() { return phoneNumber; }
        public String getEmail() { return email; }
        public String getAddress() { return address; }
        public String getCategory() { return category; }
        
        // Setters
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public void setEmail(String email) { this.email = email; }
        public void setAddress(String address) { this.address = address; }
        public void setCategory(String category) { this.category = category; }
        
        @Override
        public String toString() {
            return String.format("Contact[name='%s', phone='%s', email='%s', address='%s', category='%s']",
                               name, phoneNumber, email, address, category);
        }
        
        public String getFormattedInfo() {
            return String.format("📞 %s\n   Phone: %s\n   Email: %s\n   Address: %s\n   Category: %s",
                               name, phoneNumber, email, address, category);
        }
    }
    
    // PhoneBook class managing contacts
    static class PhoneBook {
        private Map<String, Contact> contacts; // Name -> Contact
        private Map<String, Set<Contact>> categories; // Category -> Set of Contacts
        private Map<String, Contact> phoneIndex; // Phone -> Contact (for reverse lookup)
        
        public PhoneBook() {
            this.contacts = new TreeMap<>(); // Sorted by name
            this.categories = new HashMap<>();
            this.phoneIndex = new HashMap<>();
        }
        
        // Add contact
        public boolean addContact(Contact contact) {
            String name = contact.getName().toLowerCase();
            
            if (contacts.containsKey(name)) {
                System.out.println("❌ Contact '" + contact.getName() + "' already exists!");
                return false;
            }
            
            // Add to main contacts
            contacts.put(name, contact);
            
            // Add to phone index
            phoneIndex.put(contact.getPhoneNumber(), contact);
            
            // Add to category index
            categories.computeIfAbsent(contact.getCategory(), k -> new HashSet<>()).add(contact);
            
            System.out.println("✅ Contact '" + contact.getName() + "' added successfully!");
            return true;
        }
        
        // Find contact by name
        public Contact findContactByName(String name) {
            return contacts.get(name.toLowerCase());
        }
        
        // Find contact by phone number
        public Contact findContactByPhone(String phoneNumber) {
            return phoneIndex.get(phoneNumber);
        }
        
        // Update contact information
        public boolean updateContact(String name, String newPhone, String newEmail, String newAddress) {
            Contact contact = findContactByName(name);
            if (contact == null) {
                System.out.println("❌ Contact '" + name + "' not found!");
                return false;
            }
            
            // Update phone index if phone number changed
            if (!contact.getPhoneNumber().equals(newPhone)) {
                phoneIndex.remove(contact.getPhoneNumber());
                phoneIndex.put(newPhone, contact);
                contact.setPhoneNumber(newPhone);
            }
            
            contact.setEmail(newEmail);
            contact.setAddress(newAddress);
            
            System.out.println("✅ Contact '" + name + "' updated successfully!");
            return true;
        }
        
        // Delete contact
        public boolean deleteContact(String name) {
            Contact contact = contacts.remove(name.toLowerCase());
            if (contact == null) {
                System.out.println("❌ Contact '" + name + "' not found!");
                return false;
            }
            
            // Remove from phone index
            phoneIndex.remove(contact.getPhoneNumber());
            
            // Remove from category index
            Set<Contact> categoryContacts = categories.get(contact.getCategory());
            if (categoryContacts != null) {
                categoryContacts.remove(contact);
                if (categoryContacts.isEmpty()) {
                    categories.remove(contact.getCategory());
                }
            }
            
            System.out.println("✅ Contact '" + contact.getName() + "' deleted successfully!");
            return true;
        }
        
        // List all contacts
        public void listAllContacts() {
            if (contacts.isEmpty()) {
                System.out.println("📱 Phone book is empty!");
                return;
            }
            
            System.out.println("📱 All Contacts (sorted by name):");
            System.out.println("=" + "=".repeat(60));
            
            for (Contact contact : contacts.values()) {
                System.out.println(contact.getFormattedInfo());
                System.out.println("-".repeat(50));
            }
            
            System.out.println("Total contacts: " + contacts.size());
        }
        
        // List contacts by category
        public void listContactsByCategory(String category) {
            Set<Contact> categoryContacts = categories.get(category);
            
            if (categoryContacts == null || categoryContacts.isEmpty()) {
                System.out.println("📂 No contacts found in category: " + category);
                return;
            }
            
            System.out.println("📂 Contacts in category '" + category + "':");
            System.out.println("-".repeat(50));
            
            // Sort by name for display
            List<Contact> sortedContacts = new ArrayList<>(categoryContacts);
            sortedContacts.sort(Comparator.comparing(Contact::getName));
            
            for (Contact contact : sortedContacts) {
                System.out.println(contact.getFormattedInfo());
                System.out.println("-".repeat(30));
            }
            
            System.out.println("Contacts in " + category + ": " + categoryContacts.size());
        }
        
        // Search contacts by partial name
        public void searchContacts(String searchTerm) {
            String searchLower = searchTerm.toLowerCase();
            List<Contact> matches = new ArrayList<>();
            
            for (Contact contact : contacts.values()) {
                if (contact.getName().toLowerCase().contains(searchLower)) {
                    matches.add(contact);
                }
            }
            
            if (matches.isEmpty()) {
                System.out.println("🔍 No contacts found matching: " + searchTerm);
                return;
            }
            
            System.out.println("🔍 Search results for '" + searchTerm + "':");
            System.out.println("-".repeat(50));
            
            for (Contact contact : matches) {
                System.out.println(contact.getFormattedInfo());
                System.out.println("-".repeat(30));
            }
            
            System.out.println("Found " + matches.size() + " matching contact(s)");
        }
        
        // Get statistics
        public void printStatistics() {
            System.out.println("📊 Phone Book Statistics:");
            System.out.println("-".repeat(30));
            System.out.println("Total contacts: " + contacts.size());
            System.out.println("Total categories: " + categories.size());
            
            if (!categories.isEmpty()) {
                System.out.println("\nContacts by category:");
                
                // Sort categories by contact count (descending)
                List<Map.Entry<String, Set<Contact>>> sortedCategories = new ArrayList<>(categories.entrySet());
                sortedCategories.sort((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size()));
                
                for (Map.Entry<String, Set<Contact>> entry : sortedCategories) {
                    System.out.println("  " + entry.getKey() + ": " + entry.getValue().size() + " contacts");
                }
            }
        }
        
        // Export contacts to simple format
        public void exportContacts() {
            System.out.println("\n📤 Export Format (CSV):");
            System.out.println("Name,Phone,Email,Address,Category");
            System.out.println("-".repeat(80));
            
            for (Contact contact : contacts.values()) {
                System.out.printf("%s,%s,%s,%s,%s%n",
                                contact.getName(),
                                contact.getPhoneNumber(),
                                contact.getEmail(),
                                contact.getAddress(),
                                contact.getCategory());
            }
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Phone Book Application Demo ===\n");
        
        PhoneBook phoneBook = new PhoneBook();
        
        // Add sample contacts
        addSampleContacts(phoneBook);
        
        // Demonstrate various operations
        demonstratePhoneBookOperations(phoneBook);
    }
    
    private static void addSampleContacts(PhoneBook phoneBook) {
        System.out.println("📱 Adding sample contacts:");
        System.out.println("-".repeat(30));
        
        Contact[] contacts = {
            new Contact("Alice Johnson", "555-0123", "alice@email.com", "123 Main St", "Family"),
            new Contact("Bob Smith", "555-0456", "bob@work.com", "456 Oak Ave", "Work"),
            new Contact("Carol Davis", "555-0789", "carol@friend.com", "789 Pine St", "Friend"),
            new Contact("David Wilson", "555-0111", "david@email.com", "111 Elm St", "Family"),
            new Contact("Eve Brown", "555-0222", "eve@company.com", "222 Maple Dr", "Work"),
            new Contact("Frank Miller", "555-0333", "frank@buddy.com", "333 Cedar Ln", "Friend")
        };
        
        for (Contact contact : contacts) {
            phoneBook.addContact(contact);
        }
        
        System.out.println();
    }
    
    private static void demonstratePhoneBookOperations(PhoneBook phoneBook) {
        // List all contacts
        phoneBook.listAllContacts();
        
        System.out.println("\n" + "=".repeat(70) + "\n");
        
        // Search by name
        System.out.println("🔍 Searching for 'alice':");
        Contact alice = phoneBook.findContactByName("alice");
        if (alice != null) {
            System.out.println("Found: " + alice.getFormattedInfo());
        }
        
        System.out.println("\n" + "-".repeat(50) + "\n");
        
        // Search by phone
        System.out.println("🔍 Searching by phone '555-0456':");
        Contact byPhone = phoneBook.findContactByPhone("555-0456");
        if (byPhone != null) {
            System.out.println("Found: " + byPhone.getFormattedInfo());
        }
        
        System.out.println("\n" + "-".repeat(50) + "\n");
        
        // Update contact
        System.out.println("📝 Updating Alice's information:");
        phoneBook.updateContact("Alice Johnson", "555-9999", "alice.new@email.com", "999 New Street");
        
        System.out.println("\n" + "-".repeat(50) + "\n");
        
        // List by category
        phoneBook.listContactsByCategory("Work");
        
        System.out.println("\n" + "-".repeat(50) + "\n");
        
        // Search contacts
        phoneBook.searchContacts("an");
        
        System.out.println("\n" + "-".repeat(50) + "\n");
        
        // Delete contact
        System.out.println("🗑️ Deleting contact:");
        phoneBook.deleteContact("Frank Miller");
        
        System.out.println("\n" + "-".repeat(50) + "\n");
        
        // Show statistics
        phoneBook.printStatistics();
        
        System.out.println("\n" + "-".repeat(50) + "\n");
        
        // Export
        phoneBook.exportContacts();
    }
}
```

---

## ⚡ Part 5: Task Priority Management System (30 minutes)

### **Priority Task Management with Queues**

Create `TaskManagementSystem.java`:

```java
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Task Priority Management System using Queues and Priority Queues
 * Demonstrates practical use of different queue implementations
 */
public class TaskManagementSystem {
    
    // Task class representing individual tasks
    static class Task {
        private String id;
        private String title;
        private String description;
        private Priority priority;
        private Status status;
        private LocalDateTime createdAt;
        private LocalDateTime dueDate;
        private String assignee;
        
        public enum Priority {
            LOW(1), MEDIUM(2), HIGH(3), CRITICAL(4);
            
            private final int value;
            Priority(int value) { this.value = value; }
            public int getValue() { return value; }
        }
        
        public enum Status {
            PENDING, IN_PROGRESS, COMPLETED, CANCELLED
        }
        
        public Task(String id, String title, String description, Priority priority, 
                   LocalDateTime dueDate, String assignee) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.priority = priority;
            this.status = Status.PENDING;
            this.createdAt = LocalDateTime.now();
            this.dueDate = dueDate;
            this.assignee = assignee;
        }
        
        // Getters
        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public Priority getPriority() { return priority; }
        public Status getStatus() { return status; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getDueDate() { return dueDate; }
        public String getAssignee() { return assignee; }
        
        // Setters
        public void setStatus(Status status) { this.status = status; }
        public void setPriority(Priority priority) { this.priority = priority; }
        
        public String getFormattedInfo() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            return String.format(
                "🎯 Task: %s [%s]\n" +
                "   ID: %s\n" +
                "   Description: %s\n" +
                "   Priority: %s\n" +
                "   Status: %s\n" +
                "   Assignee: %s\n" +
                "   Created: %s\n" +
                "   Due: %s",
                title, id, id, description, priority, status, assignee,
                createdAt.format(formatter), dueDate.format(formatter)
            );
        }
        
        @Override
        public String toString() {
            return String.format("Task[%s: %s (%s)]", id, title, priority);
        }
    }
    
    // Task Manager class using various queue implementations
    static class TaskManager {
        private PriorityQueue<Task> priorityQueue; // For priority-based processing
        private Queue<Task> fifoQueue; // For FIFO processing
        private Deque<Task> urgentTasks; // For urgent task stack
        private Map<String, Task> allTasks; // For quick lookup
        private Map<String, Queue<Task>> assigneeQueues; // Per-assignee queues
        
        public TaskManager() {
            // Priority queue ordered by priority (highest first), then by due date
            this.priorityQueue = new PriorityQueue<>(
                Comparator.comparing((Task t) -> t.getPriority().getValue()).reversed()
                         .thenComparing(Task::getDueDate)
            );
            
            this.fifoQueue = new LinkedList<>();
            this.urgentTasks = new ArrayDeque<>();
            this.allTasks = new HashMap<>();
            this.assigneeQueues = new HashMap<>();
        }
        
        // Add new task
        public void addTask(Task task) {
            // Add to all relevant collections
            allTasks.put(task.getId(), task);
            priorityQueue.offer(task);
            fifoQueue.offer(task);
            
            // Add to assignee queue
            assigneeQueues.computeIfAbsent(task.getAssignee(), k -> new LinkedList<>()).offer(task);
            
            // Add to urgent stack if critical
            if (task.getPriority() == Task.Priority.CRITICAL) {
                urgentTasks.push(task);
            }
            
            System.out.println("✅ Task added: " + task.getTitle());
        }
        
        // Process next task by priority
        public Task processNextTaskByPriority() {
            Task task = priorityQueue.poll();
            if (task != null && task.getStatus() == Task.Status.PENDING) {
                task.setStatus(Task.Status.IN_PROGRESS);
                System.out.println("🚀 Processing by priority: " + task.getTitle());
                return task;
            }
            return null;
        }
        
        // Process next task by FIFO
        public Task processNextTaskByFIFO() {
            Task task = fifoQueue.poll();
            if (task != null && task.getStatus() == Task.Status.PENDING) {
                task.setStatus(Task.Status.IN_PROGRESS);
                System.out.println("🚀 Processing by FIFO: " + task.getTitle());
                return task;
            }
            return null;
        }
        
        // Process urgent task (LIFO)
        public Task processUrgentTask() {
            Task task = urgentTasks.poll();
            if (task != null && task.getStatus() == Task.Status.PENDING) {
                task.setStatus(Task.Status.IN_PROGRESS);
                System.out.println("🔥 Processing urgent task: " + task.getTitle());
                return task;
            }
            return null;
        }
        
        // Get next task for specific assignee
        public Task getNextTaskForAssignee(String assignee) {
            Queue<Task> assigneeQueue = assigneeQueues.get(assignee);
            if (assigneeQueue != null && !assigneeQueue.isEmpty()) {
                Task task = assigneeQueue.poll();
                if (task.getStatus() == Task.Status.PENDING) {
                    task.setStatus(Task.Status.IN_PROGRESS);
                    System.out.println("👤 " + assignee + " assigned task: " + task.getTitle());
                    return task;
                }
            }
            return null;
        }
        
        // Complete task
        public void completeTask(String taskId) {
            Task task = allTasks.get(taskId);
            if (task != null) {
                task.setStatus(Task.Status.COMPLETED);
                System.out.println("✅ Task completed: " + task.getTitle());
            }
        }
        
        // Cancel task
        public void cancelTask(String taskId) {
            Task task = allTasks.get(taskId);
            if (task != null) {
                task.setStatus(Task.Status.CANCELLED);
                System.out.println("❌ Task cancelled: " + task.getTitle());
            }
        }
        
        // Display all tasks
        public void displayAllTasks() {
            if (allTasks.isEmpty()) {
                System.out.println("📋 No tasks in the system.");
                return;
            }
            
            System.out.println("📋 All Tasks:");
            System.out.println("=" + "=".repeat(80));
            
            for (Task task : allTasks.values()) {
                System.out.println(task.getFormattedInfo());
                System.out.println("-".repeat(60));
            }
        }
        
        // Display priority queue
        public void displayPriorityQueue() {
            if (priorityQueue.isEmpty()) {
                System.out.println("🎯 Priority queue is empty.");
                return;
            }
            
            System.out.println("🎯 Priority Queue (next to process):");
            System.out.println("-".repeat(50));
            
            // Create temporary queue to display without modifying original
            PriorityQueue<Task> tempQueue = new PriorityQueue<>(priorityQueue);
            int position = 1;
            
            while (!tempQueue.isEmpty()) {
                Task task = tempQueue.poll();
                if (task.getStatus() == Task.Status.PENDING) {
                    System.out.printf("%d. %s [%s] - %s%n", 
                                    position++, task.getTitle(), task.getPriority(), task.getAssignee());
                }
            }
        }
        
        // Display tasks by assignee
        public void displayTasksByAssignee() {
            if (assigneeQueues.isEmpty()) {
                System.out.println("👥 No tasks assigned.");
                return;
            }
            
            System.out.println("👥 Tasks by Assignee:");
            System.out.println("-".repeat(50));
            
            for (Map.Entry<String, Queue<Task>> entry : assigneeQueues.entrySet()) {
                String assignee = entry.getKey();
                Queue<Task> tasks = entry.getValue();
                
                long pendingCount = tasks.stream()
                                        .filter(t -> t.getStatus() == Task.Status.PENDING)
                                        .count();
                
                System.out.printf("%s: %d pending task(s)%n", assignee, pendingCount);
                
                tasks.stream()
                     .filter(t -> t.getStatus() == Task.Status.PENDING)
                     .forEach(t -> System.out.printf("  - %s [%s]%n", t.getTitle(), t.getPriority()));
            }
        }
        
        // Get statistics
        public void displayStatistics() {
            Map<Task.Status, Long> statusCounts = new EnumMap<>(Task.Status.class);
            Map<Task.Priority, Long> priorityCounts = new EnumMap<>(Task.Priority.class);
            
            for (Task task : allTasks.values()) {
                statusCounts.merge(task.getStatus(), 1L, Long::sum);
                priorityCounts.merge(task.getPriority(), 1L, Long::sum);
            }
            
            System.out.println("📊 Task Statistics:");
            System.out.println("-".repeat(30));
            System.out.println("Total tasks: " + allTasks.size());
            
            System.out.println("\nBy Status:");
            for (Task.Status status : Task.Status.values()) {
                long count = statusCounts.getOrDefault(status, 0L);
                System.out.printf("  %s: %d%n", status, count);
            }
            
            System.out.println("\nBy Priority:");
            for (Task.Priority priority : Task.Priority.values()) {
                long count = priorityCounts.getOrDefault(priority, 0L);
                System.out.printf("  %s: %d%n", priority, count);
            }
            
            System.out.println("\nQueue Status:");
            System.out.printf("  Priority Queue: %d tasks%n", priorityQueue.size());
            System.out.printf("  FIFO Queue: %d tasks%n", fifoQueue.size());
            System.out.printf("  Urgent Stack: %d tasks%n", urgentTasks.size());
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Task Management System Demo ===\n");
        
        TaskManager taskManager = new TaskManager();
        
        // Add sample tasks
        addSampleTasks(taskManager);
        
        // Demonstrate task management operations
        demonstrateTaskManagement(taskManager);
    }
    
    private static void addSampleTasks(TaskManager taskManager) {
        System.out.println("📝 Adding sample tasks:");
        System.out.println("-".repeat(30));
        
        LocalDateTime now = LocalDateTime.now();
        
        Task[] tasks = {
            new Task("T001", "Fix critical bug", "System crash in production", 
                    Task.Priority.CRITICAL, now.plusHours(2), "Alice"),
            new Task("T002", "Update documentation", "Update API documentation", 
                    Task.Priority.LOW, now.plusDays(3), "Bob"),
            new Task("T003", "Implement new feature", "Add user authentication", 
                    Task.Priority.HIGH, now.plusDays(1), "Carol"),
            new Task("T004", "Code review", "Review pull request #123", 
                    Task.Priority.MEDIUM, now.plusHours(4), "Alice"),
            new Task("T005", "Database migration", "Migrate user data", 
                    Task.Priority.CRITICAL, now.plusHours(1), "David"),
            new Task("T006", "Write unit tests", "Add tests for user service", 
                    Task.Priority.MEDIUM, now.plusDays(2), "Bob")
        };
        
        for (Task task : tasks) {
            taskManager.addTask(task);
        }
        
        System.out.println();
    }
    
    private static void demonstrateTaskManagement(TaskManager taskManager) {
        // Display all tasks
        taskManager.displayAllTasks();
        
        System.out.println("\n" + "=".repeat(80) + "\n");
        
        // Show priority queue
        taskManager.displayPriorityQueue();
        
        System.out.println("\n" + "-".repeat(50) + "\n");
        
        // Process tasks by priority
        System.out.println("🎯 Processing tasks by priority:");
        for (int i = 0; i < 3; i++) {
            Task task = taskManager.processNextTaskByPriority();
            if (task == null) break;
        }
        
        System.out.println("\n" + "-".repeat(50) + "\n");
        
        // Process urgent task
        System.out.println("🔥 Processing urgent tasks:");
        Task urgentTask = taskManager.processUrgentTask();
        if (urgentTask != null) {
            taskManager.completeTask(urgentTask.getId());
        }
        
        System.out.println("\n" + "-".repeat(50) + "\n");
        
        // Get task for specific assignee
        System.out.println("👤 Getting next task for Alice:");
        Task aliceTask = taskManager.getNextTaskForAssignee("Alice");
        if (aliceTask != null) {
            taskManager.completeTask(aliceTask.getId());
        }
        
        System.out.println("\n" + "-".repeat(50) + "\n");
        
        // Display tasks by assignee
        taskManager.displayTasksByAssignee();
        
        System.out.println("\n" + "-".repeat(50) + "\n");
        
        // Show statistics
        taskManager.displayStatistics();
        
        System.out.println("\n" + "-".repeat(50) + "\n");
        
        // Process remaining tasks by FIFO
        System.out.println("📋 Processing remaining tasks by FIFO:");
        Task fifoTask = taskManager.processNextTaskByFIFO();
        if (fifoTask != null) {
            taskManager.completeTask(fifoTask.getId());
        }
        
        System.out.println("\n" + "-".repeat(50) + "\n");
        
        // Final statistics
        taskManager.displayStatistics();
    }
}
```

---

## 📝 Key Takeaways from Day 11

### **Map Interface:**
- ✅ **HashMap:** Fast O(1) operations, no ordering, allows one null key
- ✅ **LinkedHashMap:** Maintains insertion order, good for LRU caches
- ✅ **TreeMap:** Sorted by keys, O(log n) operations, no null keys
- ✅ **Navigation Methods:** TreeMap provides firstKey(), lastKey(), subMap(), etc.

### **Queue Interface:**
- ✅ **LinkedList:** Implements both List and Queue, FIFO operations
- ✅ **PriorityQueue:** Heap-based, processes elements by priority
- ✅ **ArrayDeque:** Efficient double-ended queue, can be used as stack
- ✅ **Operations:** offer/poll for safe operations, add/remove for exceptions

### **Custom Sorting:**
- ✅ **Comparable:** Natural ordering implemented in the class itself
- ✅ **Comparator:** External sorting logic, multiple sorting strategies
- ✅ **Method References:** Person::getName for clean comparator syntax
- ✅ **Chaining:** thenComparing() for multi-field sorting

### **Real-World Applications:**
- ✅ **Phone Book:** Comprehensive contact management with multiple indexes
- ✅ **Task Management:** Priority-based task processing with queues
- ✅ **Search and Filter:** Efficient lookups using different data structures
- ✅ **Statistics:** Data analysis using collection stream operations

---

## 🎯 Today's Success Criteria

**Completed Tasks:**
- [ ] Master HashMap, TreeMap, and LinkedHashMap with practical examples
- [ ] Implement Queue operations for task management scenarios
- [ ] Create custom sorting with Comparable and Comparator interfaces
- [ ] Build Phone Book Application with advanced Map operations
- [ ] Develop Task Priority Management System using various Queue types
- [ ] Understand performance characteristics of different implementations

**Bonus Achievements:**
- [ ] Implement LRU cache using LinkedHashMap
- [ ] Create multi-level sorting with complex criteria
- [ ] Add search and filter functionality to applications
- [ ] Experiment with concurrent collections (ConcurrentHashMap)
- [ ] Build additional data management applications

---

## 🚀 Tomorrow's Preview (Day 12 - July 21)

**Focus:** Generics and Type Safety
- Generic classes and interfaces
- Bounded type parameters and wildcards
- Generic methods and constructors
- Type erasure and bridge methods
- Building type-safe data structures

---

## 🎉 Outstanding Progress!

You've mastered advanced collections and built sophisticated data management systems! Maps and Queues provide powerful tools for organizing and processing data efficiently. Tomorrow we'll explore Generics for type-safe programming.

**Keep building amazing applications!** 🚀