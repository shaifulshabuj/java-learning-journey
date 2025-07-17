# Day 37: Stream API Fundamentals (August 15, 2025)

**Date:** August 15, 2025  
**Duration:** 3 hours  
**Focus:** Master Stream operations for powerful data processing and transformation

---

## 🎯 Today's Learning Goals

By the end of this session, you'll:

- ✅ Understand Stream API concepts and benefits over traditional iteration
- ✅ Master Stream creation methods and intermediate operations
- ✅ Learn terminal operations and collectors for data aggregation
- ✅ Implement filtering, mapping, and reduction operations
- ✅ Apply Stream debugging techniques and performance considerations
- ✅ Build practical data processing applications using Streams

---

## 🚀 Part 1: Stream Creation & Basic Operations (60 minutes)

### **Understanding the Stream API**

**Stream** represents a sequence of elements supporting sequential and parallel aggregate operations:

- **Not a data structure**: Streams don't store elements
- **Functional**: Operations are pure functions without side effects
- **Lazy evaluation**: Intermediate operations are not executed until terminal operation
- **Possibly infinite**: Streams can represent unbounded sequences
- **Consumable**: Each stream can only be operated on once

### **Stream Creation and Basic Operations**

Create `StreamBasics.java`:

```java
import java.util.*;
import java.util.stream.*;
import java.nio.file.*;
import java.io.IOException;

/**
 * Comprehensive demonstration of Stream API fundamentals
 */
public class StreamBasics {
    
    public static void main(String[] args) {
        System.out.println("=== Stream API Fundamentals ===\n");
        
        // 1. Stream creation methods
        System.out.println("1. Stream Creation Methods:");
        demonstrateStreamCreation();
        
        // 2. Intermediate operations
        System.out.println("\n2. Intermediate Operations:");
        demonstrateIntermediateOperations();
        
        // 3. Terminal operations
        System.out.println("\n3. Terminal Operations:");
        demonstrateTerminalOperations();
        
        // 4. Method chaining and pipelines
        System.out.println("\n4. Stream Pipelines:");
        demonstrateStreamPipelines();
        
        System.out.println("\n=== Stream Basics Completed ===");
    }
    
    private static void demonstrateStreamCreation() {
        // 1. From collections
        List<String> list = Arrays.asList("apple", "banana", "cherry", "date");
        Stream<String> streamFromList = list.stream();
        System.out.println("From List: " + streamFromList.collect(Collectors.toList()));
        
        // 2. From arrays
        String[] array = {"red", "green", "blue"};
        Stream<String> streamFromArray = Arrays.stream(array);
        System.out.println("From Array: " + streamFromArray.collect(Collectors.toList()));
        
        // 3. Using Stream.of()
        Stream<Integer> streamFromOf = Stream.of(1, 2, 3, 4, 5);
        System.out.println("From Stream.of(): " + streamFromOf.collect(Collectors.toList()));
        
        // 4. Empty stream
        Stream<String> emptyStream = Stream.empty();
        System.out.println("Empty stream count: " + emptyStream.count());
        
        // 5. Infinite streams
        Stream<Integer> infiniteNumbers = Stream.iterate(0, n -> n + 2);
        List<Integer> first10Evens = infiniteNumbers.limit(10).collect(Collectors.toList());
        System.out.println("First 10 even numbers: " + first10Evens);
        
        // 6. Generate stream
        Stream<Double> randomNumbers = Stream.generate(Math::random);
        List<Double> first5Random = randomNumbers.limit(5).collect(Collectors.toList());
        System.out.println("First 5 random numbers: " + first5Random);
        
        // 7. Range streams
        IntStream intRange = IntStream.range(1, 6);
        System.out.println("IntStream range 1-5: " + intRange.boxed().collect(Collectors.toList()));
        
        IntStream intRangeClosed = IntStream.rangeClosed(1, 5);
        System.out.println("IntStream rangeClosed 1-5: " + intRangeClosed.boxed().collect(Collectors.toList()));
        
        // 8. From strings
        IntStream charStream = "Hello".chars();
        System.out.println("Characters as ints: " + charStream.boxed().collect(Collectors.toList()));
        
        // 9. From file lines (commented out to avoid file dependency)
        // Stream<String> fileLines = Files.lines(Paths.get("sample.txt"));
        
        // 10. Builder pattern
        Stream<String> builtStream = Stream.<String>builder()
            .add("first")
            .add("second")
            .add("third")
            .build();
        System.out.println("Built stream: " + builtStream.collect(Collectors.toList()));
    }
    
    private static void demonstrateIntermediateOperations() {
        List<String> words = Arrays.asList("apple", "banana", "cherry", "date", "elderberry", "fig");
        System.out.println("Original words: " + words);
        
        // 1. filter() - select elements based on predicate
        List<String> longWords = words.stream()
            .filter(word -> word.length() > 5)
            .collect(Collectors.toList());
        System.out.println("Long words (>5 chars): " + longWords);
        
        // 2. map() - transform elements
        List<Integer> wordLengths = words.stream()
            .map(String::length)
            .collect(Collectors.toList());
        System.out.println("Word lengths: " + wordLengths);
        
        List<String> upperCaseWords = words.stream()
            .map(String::toUpperCase)
            .collect(Collectors.toList());
        System.out.println("Uppercase words: " + upperCaseWords);
        
        // 3. distinct() - remove duplicates
        List<String> wordsWithDuplicates = Arrays.asList("apple", "banana", "apple", "cherry", "banana");
        List<String> uniqueWords = wordsWithDuplicates.stream()
            .distinct()
            .collect(Collectors.toList());
        System.out.println("Unique words: " + uniqueWords);
        
        // 4. sorted() - sort elements
        List<String> sortedWords = words.stream()
            .sorted()
            .collect(Collectors.toList());
        System.out.println("Sorted alphabetically: " + sortedWords);
        
        List<String> sortedByLength = words.stream()
            .sorted(Comparator.comparing(String::length))
            .collect(Collectors.toList());
        System.out.println("Sorted by length: " + sortedByLength);
        
        // 5. limit() - limit number of elements
        List<String> firstThree = words.stream()
            .limit(3)
            .collect(Collectors.toList());
        System.out.println("First 3 words: " + firstThree);
        
        // 6. skip() - skip elements
        List<String> skipFirst = words.stream()
            .skip(2)
            .collect(Collectors.toList());
        System.out.println("Skip first 2: " + skipFirst);
        
        // 7. peek() - perform action without changing stream
        List<String> peekedWords = words.stream()
            .peek(word -> System.out.println("Processing: " + word))
            .filter(word -> word.startsWith("a"))
            .collect(Collectors.toList());
        System.out.println("Words starting with 'a': " + peekedWords);
        
        // 8. flatMap() - flatten nested structures
        List<List<String>> nestedLists = Arrays.asList(
            Arrays.asList("a", "b"),
            Arrays.asList("c", "d"),
            Arrays.asList("e", "f")
        );
        List<String> flattenedList = nestedLists.stream()
            .flatMap(List::stream)
            .collect(Collectors.toList());
        System.out.println("Flattened lists: " + flattenedList);
        
        // Complex example: words to characters
        List<String> characters = words.stream()
            .flatMap(word -> word.chars().mapToObj(c -> String.valueOf((char) c)))
            .distinct()
            .sorted()
            .collect(Collectors.toList());
        System.out.println("All unique characters: " + characters);
    }
    
    private static void demonstrateTerminalOperations() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        System.out.println("Numbers: " + numbers);
        
        // 1. collect() - collect to collection
        List<Integer> evenNumbers = numbers.stream()
            .filter(n -> n % 2 == 0)
            .collect(Collectors.toList());
        System.out.println("Even numbers: " + evenNumbers);
        
        Set<Integer> uniqueNumbers = numbers.stream()
            .collect(Collectors.toSet());
        System.out.println("As set: " + uniqueNumbers);
        
        // 2. forEach() - perform action on each element
        System.out.print("Numbers squared: ");
        numbers.stream()
            .map(n -> n * n)
            .forEach(n -> System.out.print(n + " "));
        System.out.println();
        
        // 3. reduce() - reduce to single value
        Optional<Integer> sum = numbers.stream()
            .reduce((a, b) -> a + b);
        System.out.println("Sum using reduce: " + sum.orElse(0));
        
        Optional<Integer> max = numbers.stream()
            .reduce(Integer::max);
        System.out.println("Max using reduce: " + max.orElse(0));
        
        // reduce with identity
        Integer product = numbers.stream()
            .reduce(1, (a, b) -> a * b);
        System.out.println("Product: " + product);
        
        // 4. count() - count elements
        long count = numbers.stream()
            .filter(n -> n > 5)
            .count();
        System.out.println("Count of numbers > 5: " + count);
        
        // 5. anyMatch(), allMatch(), noneMatch()
        boolean hasEven = numbers.stream().anyMatch(n -> n % 2 == 0);
        boolean allPositive = numbers.stream().allMatch(n -> n > 0);
        boolean noneNegative = numbers.stream().noneMatch(n -> n < 0);
        
        System.out.println("Has even numbers: " + hasEven);
        System.out.println("All positive: " + allPositive);
        System.out.println("None negative: " + noneNegative);
        
        // 6. findFirst(), findAny()
        Optional<Integer> firstEven = numbers.stream()
            .filter(n -> n % 2 == 0)
            .findFirst();
        System.out.println("First even number: " + firstEven.orElse(-1));
        
        Optional<Integer> anyOdd = numbers.stream()
            .filter(n -> n % 2 == 1)
            .findAny();
        System.out.println("Any odd number: " + anyOdd.orElse(-1));
        
        // 7. min(), max()
        Optional<Integer> minimum = numbers.stream().min(Integer::compareTo);
        Optional<Integer> maximum = numbers.stream().max(Integer::compareTo);
        
        System.out.println("Minimum: " + minimum.orElse(0));
        System.out.println("Maximum: " + maximum.orElse(0));
        
        // 8. toArray()
        Integer[] numberArray = numbers.stream()
            .filter(n -> n % 2 == 0)
            .toArray(Integer[]::new);
        System.out.println("Even numbers as array: " + Arrays.toString(numberArray));
    }
    
    private static void demonstrateStreamPipelines() {
        List<String> sentences = Arrays.asList(
            "The quick brown fox jumps over the lazy dog",
            "Java streams are powerful and expressive",
            "Functional programming makes code more readable",
            "Lambda expressions simplify complex operations"
        );
        
        System.out.println("Original sentences:");
        sentences.forEach(System.out::println);
        
        // Complex pipeline: extract unique words, longer than 4 chars, sorted by length
        List<String> processedWords = sentences.stream()
            .flatMap(sentence -> Arrays.stream(sentence.split("\\s+")))  // Split into words
            .map(word -> word.replaceAll("[^a-zA-Z]", ""))              // Remove punctuation
            .filter(word -> !word.isEmpty())                            // Remove empty strings
            .map(String::toLowerCase)                                   // Convert to lowercase
            .filter(word -> word.length() > 4)                         // Filter long words
            .distinct()                                                 // Remove duplicates
            .sorted(Comparator.comparing(String::length)               // Sort by length
                .thenComparing(String::compareTo))                     // Then alphabetically
            .collect(Collectors.toList());
        
        System.out.println("\nProcessed words (unique, >4 chars, sorted):");
        processedWords.forEach(System.out::println);
        
        // Statistics pipeline
        IntSummaryStatistics stats = sentences.stream()
            .mapToInt(String::length)
            .summaryStatistics();
        
        System.out.println("\nSentence length statistics:");
        System.out.println("Count: " + stats.getCount());
        System.out.println("Sum: " + stats.getSum());
        System.out.println("Min: " + stats.getMin());
        System.out.println("Max: " + stats.getMax());
        System.out.println("Average: " + stats.getAverage());
        
        // Grouping pipeline
        Map<Integer, List<String>> wordsByLength = processedWords.stream()
            .collect(Collectors.groupingBy(String::length));
        
        System.out.println("\nWords grouped by length:");
        wordsByLength.forEach((length, words) -> 
            System.out.println(length + " chars: " + words));
        
        // Partitioning pipeline
        Map<Boolean, List<String>> partitionedWords = processedWords.stream()
            .collect(Collectors.partitioningBy(word -> word.length() > 6));
        
        System.out.println("\nWords partitioned by length > 6:");
        System.out.println("Long words: " + partitionedWords.get(true));
        System.out.println("Short words: " + partitionedWords.get(false));
    }
}
```

---

## 🔧 Part 2: Collectors & Data Aggregation (60 minutes)

### **Powerful Data Collection and Transformation**

Create `StreamCollectors.java`:

```java
import java.util.*;
import java.util.stream.*;
import java.util.function.*;

/**
 * Comprehensive demonstration of Collectors for data aggregation
 */
public class StreamCollectors {
    
    public static void main(String[] args) {
        System.out.println("=== Stream Collectors Demo ===\n");
        
        // 1. Basic collectors
        System.out.println("1. Basic Collectors:");
        demonstrateBasicCollectors();
        
        // 2. Grouping collectors
        System.out.println("\n2. Grouping Collectors:");
        demonstrateGroupingCollectors();
        
        // 3. Downstream collectors
        System.out.println("\n3. Downstream Collectors:");
        demonstrateDownstreamCollectors();
        
        // 4. Custom collectors
        System.out.println("\n4. Custom Collectors:");
        demonstrateCustomCollectors();
        
        // 5. Practical data analysis
        System.out.println("\n5. Practical Data Analysis:");
        demonstrateDataAnalysis();
        
        System.out.println("\n=== Stream Collectors Completed ===");
    }
    
    private static void demonstrateBasicCollectors() {
        List<String> words = Arrays.asList("apple", "banana", "cherry", "date", "elderberry");
        
        // 1. toList(), toSet(), toCollection()
        List<String> list = words.stream().collect(Collectors.toList());
        Set<String> set = words.stream().collect(Collectors.toSet());
        LinkedList<String> linkedList = words.stream()
            .collect(Collectors.toCollection(LinkedList::new));
        
        System.out.println("To List: " + list);
        System.out.println("To Set: " + set);
        System.out.println("To LinkedList: " + linkedList);
        
        // 2. joining()
        String joined = words.stream().collect(Collectors.joining());
        String joinedWithDelimiter = words.stream().collect(Collectors.joining(", "));
        String joinedWithPrefixSuffix = words.stream()
            .collect(Collectors.joining(", ", "[", "]"));
        
        System.out.println("Joined: " + joined);
        System.out.println("Joined with delimiter: " + joinedWithDelimiter);
        System.out.println("Joined with prefix/suffix: " + joinedWithPrefixSuffix);
        
        // 3. counting()
        long count = words.stream().collect(Collectors.counting());
        System.out.println("Count: " + count);
        
        // 4. summarizing statistics
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        IntSummaryStatistics intStats = numbers.stream()
            .collect(Collectors.summarizingInt(Integer::intValue));
        
        System.out.println("Int Statistics: " + intStats);
        
        DoubleSummaryStatistics doubleStats = words.stream()
            .collect(Collectors.summarizingDouble(String::length));
        
        System.out.println("Word Length Statistics: " + doubleStats);
        
        // 5. averaging, summing
        double averageLength = words.stream()
            .collect(Collectors.averagingDouble(String::length));
        int totalLength = words.stream()
            .collect(Collectors.summingInt(String::length));
        
        System.out.println("Average word length: " + averageLength);
        System.out.println("Total length: " + totalLength);
        
        // 6. maxBy(), minBy()
        Optional<String> longest = words.stream()
            .collect(Collectors.maxBy(Comparator.comparing(String::length)));
        Optional<String> shortest = words.stream()
            .collect(Collectors.minBy(Comparator.comparing(String::length)));
        
        System.out.println("Longest word: " + longest.orElse("none"));
        System.out.println("Shortest word: " + shortest.orElse("none"));
    }
    
    private static void demonstrateGroupingCollectors() {
        List<Person> people = Arrays.asList(
            new Person("Alice", 25, "Engineer", 75000),
            new Person("Bob", 30, "Manager", 85000),
            new Person("Charlie", 35, "Engineer", 80000),
            new Person("Diana", 28, "Designer", 70000),
            new Person("Eve", 32, "Manager", 90000),
            new Person("Frank", 29, "Engineer", 78000)
        );
        
        System.out.println("People: " + people);
        
        // 1. groupingBy() - simple grouping
        Map<String, List<Person>> byJob = people.stream()
            .collect(Collectors.groupingBy(Person::getJob));
        
        System.out.println("\nGrouped by job:");
        byJob.forEach((job, persons) -> 
            System.out.println(job + ": " + persons.size() + " people"));
        
        // 2. groupingBy() with custom classifier
        Map<String, List<Person>> byAgeGroup = people.stream()
            .collect(Collectors.groupingBy(person -> {
                if (person.getAge() < 30) return "Young";
                else if (person.getAge() < 35) return "Middle";
                else return "Senior";
            }));
        
        System.out.println("\nGrouped by age group:");
        byAgeGroup.forEach((group, persons) -> 
            System.out.println(group + ": " + persons));
        
        // 3. partitioningBy() - binary grouping
        Map<Boolean, List<Person>> byHighSalary = people.stream()
            .collect(Collectors.partitioningBy(person -> person.getSalary() > 80000));
        
        System.out.println("\nPartitioned by high salary (>80k):");
        System.out.println("High salary: " + byHighSalary.get(true));
        System.out.println("Regular salary: " + byHighSalary.get(false));
        
        // 4. Multi-level grouping
        Map<String, Map<String, List<Person>>> byJobAndAgeGroup = people.stream()
            .collect(Collectors.groupingBy(
                Person::getJob,
                Collectors.groupingBy(person -> person.getAge() < 30 ? "Young" : "Senior")
            ));
        
        System.out.println("\nMulti-level grouping (job -> age group):");
        byJobAndAgeGroup.forEach((job, ageGroups) -> {
            System.out.println(job + ":");
            ageGroups.forEach((ageGroup, persons) -> 
                System.out.println("  " + ageGroup + ": " + persons.size() + " people"));
        });
    }
    
    private static void demonstrateDownstreamCollectors() {
        List<Person> people = Arrays.asList(
            new Person("Alice", 25, "Engineer", 75000),
            new Person("Bob", 30, "Manager", 85000),
            new Person("Charlie", 35, "Engineer", 80000),
            new Person("Diana", 28, "Designer", 70000),
            new Person("Eve", 32, "Manager", 90000),
            new Person("Frank", 29, "Engineer", 78000)
        );
        
        // 1. groupingBy() with counting()
        Map<String, Long> countByJob = people.stream()
            .collect(Collectors.groupingBy(Person::getJob, Collectors.counting()));
        
        System.out.println("Count by job: " + countByJob);
        
        // 2. groupingBy() with averaging()
        Map<String, Double> avgSalaryByJob = people.stream()
            .collect(Collectors.groupingBy(
                Person::getJob, 
                Collectors.averagingDouble(Person::getSalary)
            ));
        
        System.out.println("Average salary by job: " + avgSalaryByJob);
        
        // 3. groupingBy() with summing()
        Map<String, Integer> totalSalaryByJob = people.stream()
            .collect(Collectors.groupingBy(
                Person::getJob,
                Collectors.summingInt(Person::getSalary)
            ));
        
        System.out.println("Total salary by job: " + totalSalaryByJob);
        
        // 4. groupingBy() with maxBy()
        Map<String, Optional<Person>> highestPaidByJob = people.stream()
            .collect(Collectors.groupingBy(
                Person::getJob,
                Collectors.maxBy(Comparator.comparing(Person::getSalary))
            ));
        
        System.out.println("Highest paid by job:");
        highestPaidByJob.forEach((job, person) -> 
            System.out.println(job + ": " + person.map(Person::getName).orElse("none")));
        
        // 5. groupingBy() with mapping()
        Map<String, List<String>> namesByJob = people.stream()
            .collect(Collectors.groupingBy(
                Person::getJob,
                Collectors.mapping(Person::getName, Collectors.toList())
            ));
        
        System.out.println("Names by job: " + namesByJob);
        
        // 6. groupingBy() with filtering() (Java 9+)
        Map<String, List<Person>> seniorEngineers = people.stream()
            .collect(Collectors.groupingBy(
                Person::getJob,
                Collectors.filtering(p -> p.getAge() > 30, Collectors.toList())
            ));
        
        System.out.println("Senior people by job: " + seniorEngineers);
        
        // 7. Complex downstream collector
        Map<String, String> jobSummary = people.stream()
            .collect(Collectors.groupingBy(
                Person::getJob,
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    list -> String.format("%d people, avg age %.1f", 
                        list.size(), 
                        list.stream().mapToInt(Person::getAge).average().orElse(0.0))
                )
            ));
        
        System.out.println("Job summary: " + jobSummary);
    }
    
    private static void demonstrateCustomCollectors() {
        List<String> words = Arrays.asList("apple", "banana", "cherry", "date", "elderberry");
        
        // 1. Custom collector using Collector.of()
        Collector<String, ?, String> toStringWithCount = Collector.of(
            StringBuilder::new,                           // supplier
            (sb, word) -> sb.append(word).append(";"),   // accumulator
            StringBuilder::append,                       // combiner
            sb -> sb.toString() + " (count: " + 
                (sb.toString().split(";").length - 1) + ")" // finisher
        );
        
        String result = words.stream().collect(toStringWithCount);
        System.out.println("Custom collector result: " + result);
        
        // 2. Custom collector for finding unique characters
        Collector<String, ?, Set<Character>> uniqueCharacters = Collector.of(
            HashSet::new,                                      // supplier
            (set, word) -> word.chars()                       // accumulator
                .forEach(c -> set.add((char) c)),
            (set1, set2) -> { set1.addAll(set2); return set1; }, // combiner
            Function.identity()                               // finisher (identity)
        );
        
        Set<Character> chars = words.stream().collect(uniqueCharacters);
        System.out.println("Unique characters: " + chars);
        
        // 3. Custom collector with characteristics
        Collector<String, ?, List<String>> reversedWords = Collector.of(
            ArrayList::new,
            (list, word) -> list.add(new StringBuilder(word).reverse().toString()),
            (list1, list2) -> { list1.addAll(list2); return list1; },
            Function.identity(),
            Collector.Characteristics.IDENTITY_FINISH
        );
        
        List<String> reversed = words.stream().collect(reversedWords);
        System.out.println("Reversed words: " + reversed);
        
        // 4. Immutable collection collector
        Collector<String, ?, List<String>> toImmutableList = Collector.of(
            ArrayList::new,
            List::add,
            (list1, list2) -> { list1.addAll(list2); return list1; },
            Collections::unmodifiableList
        );
        
        List<String> immutableList = words.stream().collect(toImmutableList);
        System.out.println("Immutable list: " + immutableList);
        // immutableList.add("test"); // Would throw UnsupportedOperationException
    }
    
    private static void demonstrateDataAnalysis() {
        // Sample sales data
        List<Sale> sales = Arrays.asList(
            new Sale("Alice", "Electronics", 1500, "2025-01-15"),
            new Sale("Bob", "Books", 300, "2025-01-16"),
            new Sale("Charlie", "Electronics", 2200, "2025-01-17"),
            new Sale("Diana", "Clothing", 800, "2025-01-18"),
            new Sale("Eve", "Books", 450, "2025-01-19"),
            new Sale("Frank", "Electronics", 1800, "2025-01-20"),
            new Sale("Alice", "Clothing", 600, "2025-01-21"),
            new Sale("Bob", "Electronics", 1200, "2025-01-22")
        );
        
        System.out.println("Sales data analysis:");
        
        // 1. Total sales by category
        Map<String, Integer> totalByCategory = sales.stream()
            .collect(Collectors.groupingBy(
                Sale::getCategory,
                Collectors.summingInt(Sale::getAmount)
            ));
        
        System.out.println("Total sales by category: " + totalByCategory);
        
        // 2. Average sale amount by salesperson
        Map<String, Double> avgBySalesperson = sales.stream()
            .collect(Collectors.groupingBy(
                Sale::getSalesperson,
                Collectors.averagingDouble(Sale::getAmount)
            ));
        
        System.out.println("Average sale by salesperson: " + avgBySalesperson);
        
        // 3. Top selling category
        Optional<Map.Entry<String, Integer>> topCategory = totalByCategory.entrySet()
            .stream()
            .max(Map.Entry.comparingByValue());
        
        System.out.println("Top selling category: " + 
            topCategory.map(e -> e.getKey() + " ($" + e.getValue() + ")")
                      .orElse("none"));
        
        // 4. Sales statistics
        IntSummaryStatistics salesStats = sales.stream()
            .collect(Collectors.summarizingInt(Sale::getAmount));
        
        System.out.println("Sales statistics: " + salesStats);
        
        // 5. High-value sales (>1000)
        List<Sale> highValueSales = sales.stream()
            .filter(sale -> sale.getAmount() > 1000)
            .sorted(Comparator.comparing(Sale::getAmount).reversed())
            .collect(Collectors.toList());
        
        System.out.println("High-value sales (>$1000): " + highValueSales);
        
        // 6. Salesperson performance ranking
        List<Map.Entry<String, Double>> performanceRanking = avgBySalesperson.entrySet()
            .stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .collect(Collectors.toList());
        
        System.out.println("Performance ranking:");
        performanceRanking.forEach(entry -> 
            System.out.printf("  %s: $%.2f average\n", entry.getKey(), entry.getValue()));
        
        // 7. Complex analysis: category performance by month
        Map<String, Map<String, List<Sale>>> categoryByMonth = sales.stream()
            .collect(Collectors.groupingBy(
                Sale::getCategory,
                Collectors.groupingBy(sale -> sale.getDate().substring(0, 7)) // Extract year-month
            ));
        
        System.out.println("Category performance by month: " + categoryByMonth);
    }
    
    // Helper classes
    static class Person {
        private final String name;
        private final int age;
        private final String job;
        private final int salary;
        
        public Person(String name, int age, String job, int salary) {
            this.name = name;
            this.age = age;
            this.job = job;
            this.salary = salary;
        }
        
        public String getName() { return name; }
        public int getAge() { return age; }
        public String getJob() { return job; }
        public int getSalary() { return salary; }
        
        @Override
        public String toString() {
            return String.format("%s(%d, %s, $%d)", name, age, job, salary);
        }
    }
    
    static class Sale {
        private final String salesperson;
        private final String category;
        private final int amount;
        private final String date;
        
        public Sale(String salesperson, String category, int amount, String date) {
            this.salesperson = salesperson;
            this.category = category;
            this.amount = amount;
            this.date = date;
        }
        
        public String getSalesperson() { return salesperson; }
        public String getCategory() { return category; }
        public int getAmount() { return amount; }
        public String getDate() { return date; }
        
        @Override
        public String toString() {
            return String.format("%s: %s $%d (%s)", salesperson, category, amount, date);
        }
    }
}
```

---

## 📊 Part 3: Practical Applications & Performance (60 minutes)

### **Real-World Data Processing Examples**

Create `StreamApplications.java`:

```java
import java.util.*;
import java.util.stream.*;
import java.util.function.*;
import java.time.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Practical Stream API applications for real-world scenarios
 */
public class StreamApplications {
    
    public static void main(String[] args) {
        System.out.println("=== Stream Applications Demo ===\n");
        
        // 1. Log file analysis
        System.out.println("1. Log File Analysis:");
        LogAnalyzer logAnalyzer = new LogAnalyzer();
        logAnalyzer.demonstrateLogAnalysis();
        
        // 2. Sales report generation
        System.out.println("\n2. Sales Report Generation:");
        SalesReportGenerator reportGenerator = new SalesReportGenerator();
        reportGenerator.demonstrateReportGeneration();
        
        // 3. Performance considerations
        System.out.println("\n3. Performance Considerations:");
        demonstratePerformanceConsiderations();
        
        System.out.println("\n=== Stream Applications Completed ===");
    }
    
    private static void demonstratePerformanceConsiderations() {
        List<Integer> largeList = IntStream.rangeClosed(1, 1_000_000)
            .boxed()
            .collect(Collectors.toList());
        
        System.out.println("Performance testing with " + largeList.size() + " elements");
        
        // 1. Stream vs traditional loop
        long startTime = System.currentTimeMillis();
        
        // Traditional loop
        int sumTraditional = 0;
        for (int num : largeList) {
            if (num % 2 == 0) {
                sumTraditional += num * num;
            }
        }
        long traditionalTime = System.currentTimeMillis() - startTime;
        
        // Stream approach
        startTime = System.currentTimeMillis();
        int sumStream = largeList.stream()
            .filter(n -> n % 2 == 0)
            .mapToInt(n -> n * n)
            .sum();
        long streamTime = System.currentTimeMillis() - startTime;
        
        System.out.println("Traditional loop time: " + traditionalTime + "ms, result: " + sumTraditional);
        System.out.println("Stream time: " + streamTime + "ms, result: " + sumStream);
        
        // 2. Parallel stream
        startTime = System.currentTimeMillis();
        int sumParallel = largeList.parallelStream()
            .filter(n -> n % 2 == 0)
            .mapToInt(n -> n * n)
            .sum();
        long parallelTime = System.currentTimeMillis() - startTime;
        
        System.out.println("Parallel stream time: " + parallelTime + "ms, result: " + sumParallel);
        
        // 3. Primitive streams for better performance
        startTime = System.currentTimeMillis();
        int sumPrimitive = largeList.stream()
            .mapToInt(Integer::intValue)
            .filter(n -> n % 2 == 0)
            .map(n -> n * n)
            .sum();
        long primitiveTime = System.currentTimeMillis() - startTime;
        
        System.out.println("Primitive stream time: " + primitiveTime + "ms, result: " + sumPrimitive);
        
        // 4. Debugging streams with peek()
        System.out.println("\nStream debugging example:");
        List<String> debugResult = Arrays.asList("apple", "banana", "cherry", "date")
            .stream()
            .peek(s -> System.out.println("Original: " + s))
            .filter(s -> s.length() > 4)
            .peek(s -> System.out.println("After filter: " + s))
            .map(String::toUpperCase)
            .peek(s -> System.out.println("After map: " + s))
            .collect(Collectors.toList());
        
        System.out.println("Final result: " + debugResult);
    }
}

/**
 * Log file analysis using Stream API
 */
class LogAnalyzer {
    
    public void demonstrateLogAnalysis() {
        // Simulate log entries
        List<LogEntry> logs = Arrays.asList(
            new LogEntry("2025-08-15 10:00:01", "INFO", "Application started"),
            new LogEntry("2025-08-15 10:00:05", "DEBUG", "Database connection established"),
            new LogEntry("2025-08-15 10:01:00", "INFO", "User login: alice@example.com"),
            new LogEntry("2025-08-15 10:01:30", "WARN", "Slow query detected: 2.5s"),
            new LogEntry("2025-08-15 10:02:00", "ERROR", "Database connection failed"),
            new LogEntry("2025-08-15 10:02:15", "INFO", "Retrying database connection"),
            new LogEntry("2025-08-15 10:02:20", "INFO", "Database connection restored"),
            new LogEntry("2025-08-15 10:03:00", "INFO", "User logout: alice@example.com"),
            new LogEntry("2025-08-15 10:03:30", "ERROR", "Out of memory error"),
            new LogEntry("2025-08-15 10:04:00", "FATAL", "Application crashed")
        );
        
        System.out.println("Analyzing " + logs.size() + " log entries...");
        
        // 1. Count by log level
        Map<String, Long> countByLevel = logs.stream()
            .collect(Collectors.groupingBy(LogEntry::getLevel, Collectors.counting()));
        
        System.out.println("Log count by level: " + countByLevel);
        
        // 2. Find all errors and warnings
        List<LogEntry> problemLogs = logs.stream()
            .filter(log -> log.getLevel().equals("ERROR") || log.getLevel().equals("WARN") || log.getLevel().equals("FATAL"))
            .collect(Collectors.toList());
        
        System.out.println("Problem logs:");
        problemLogs.forEach(log -> System.out.println("  " + log));
        
        // 3. Extract user activities
        List<String> userActivities = logs.stream()
            .map(LogEntry::getMessage)
            .filter(message -> message.contains("User"))
            .collect(Collectors.toList());
        
        System.out.println("User activities: " + userActivities);
        
        // 4. Time-based analysis (group by hour)
        Map<String, Long> logsByHour = logs.stream()
            .collect(Collectors.groupingBy(
                log -> log.getTimestamp().substring(11, 13), // Extract hour
                Collectors.counting()
            ));
        
        System.out.println("Logs by hour: " + logsByHour);
        
        // 5. Find critical time periods (multiple errors in sequence)
        List<LogEntry> errorSequence = logs.stream()
            .filter(log -> log.getLevel().equals("ERROR") || log.getLevel().equals("FATAL"))
            .collect(Collectors.toList());
        
        System.out.println("Critical sequence: " + errorSequence);
        
        // 6. Generate summary report
        String summary = generateLogSummary(logs);
        System.out.println("Summary report:\n" + summary);
    }
    
    private String generateLogSummary(List<LogEntry> logs) {
        Map<String, Long> levelCounts = logs.stream()
            .collect(Collectors.groupingBy(LogEntry::getLevel, Collectors.counting()));
        
        long totalLogs = logs.size();
        long errorCount = levelCounts.getOrDefault("ERROR", 0L) + levelCounts.getOrDefault("FATAL", 0L);
        double errorRate = (errorCount * 100.0) / totalLogs;
        
        return String.format(
            "Log Analysis Summary:\n" +
            "- Total logs: %d\n" +
            "- Error rate: %.2f%%\n" +
            "- Level distribution: %s\n" +
            "- Health status: %s",
            totalLogs, errorRate, levelCounts,
            errorRate > 20 ? "CRITICAL" : errorRate > 10 ? "WARNING" : "HEALTHY"
        );
    }
    
    static class LogEntry {
        private final String timestamp;
        private final String level;
        private final String message;
        
        public LogEntry(String timestamp, String level, String message) {
            this.timestamp = timestamp;
            this.level = level;
            this.message = message;
        }
        
        public String getTimestamp() { return timestamp; }
        public String getLevel() { return level; }
        public String getMessage() { return message; }
        
        @Override
        public String toString() {
            return String.format("[%s] %s: %s", timestamp, level, message);
        }
    }
}

/**
 * Sales report generation using Stream API
 */
class SalesReportGenerator {
    
    public void demonstrateReportGeneration() {
        // Generate sample sales data
        List<SalesRecord> sales = generateSalesData();
        
        System.out.println("Generating reports from " + sales.size() + " sales records...");
        
        // 1. Monthly sales summary
        Map<String, Double> monthlySales = sales.stream()
            .collect(Collectors.groupingBy(
                record -> record.getDate().substring(0, 7), // Extract year-month
                Collectors.summingDouble(SalesRecord::getAmount)
            ));
        
        System.out.println("Monthly sales summary:");
        monthlySales.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> System.out.printf("  %s: $%.2f\n", entry.getKey(), entry.getValue()));
        
        // 2. Top performers
        Map<String, Double> salesByPerson = sales.stream()
            .collect(Collectors.groupingBy(
                SalesRecord::getSalesperson,
                Collectors.summingDouble(SalesRecord::getAmount)
            ));
        
        List<Map.Entry<String, Double>> topPerformers = salesByPerson.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(3)
            .collect(Collectors.toList());
        
        System.out.println("Top 3 performers:");
        topPerformers.forEach(entry -> 
            System.out.printf("  %s: $%.2f\n", entry.getKey(), entry.getValue()));
        
        // 3. Product category analysis
        Map<String, Map<String, Double>> categoryStats = sales.stream()
            .collect(Collectors.groupingBy(
                SalesRecord::getCategory,
                Collectors.teeing(
                    Collectors.summingDouble(SalesRecord::getAmount),
                    Collectors.averagingDouble(SalesRecord::getAmount),
                    (total, avg) -> Map.of("total", total, "average", avg)
                )
            ));
        
        System.out.println("Category analysis:");
        categoryStats.forEach((category, stats) -> 
            System.out.printf("  %s: Total $%.2f, Average $%.2f\n", 
                category, stats.get("total"), stats.get("average")));
        
        // 4. Quarterly trends
        Map<String, Double> quarterlyTrends = sales.stream()
            .collect(Collectors.groupingBy(
                this::getQuarter,
                Collectors.summingDouble(SalesRecord::getAmount)
            ));
        
        System.out.println("Quarterly trends: " + quarterlyTrends);
        
        // 5. Sales efficiency metrics
        Map<String, Long> transactionCount = sales.stream()
            .collect(Collectors.groupingBy(SalesRecord::getSalesperson, Collectors.counting()));
        
        Map<String, Double> efficiency = salesByPerson.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue() / transactionCount.get(entry.getKey())
            ));
        
        System.out.println("Sales efficiency (average per transaction):");
        efficiency.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .forEach(entry -> 
                System.out.printf("  %s: $%.2f per transaction\n", entry.getKey(), entry.getValue()));
    }
    
    private String getQuarter(SalesRecord record) {
        String month = record.getDate().substring(5, 7);
        int monthNum = Integer.parseInt(month);
        String year = record.getDate().substring(0, 4);
        
        if (monthNum <= 3) return year + "-Q1";
        else if (monthNum <= 6) return year + "-Q2";
        else if (monthNum <= 9) return year + "-Q3";
        else return year + "-Q4";
    }
    
    private List<SalesRecord> generateSalesData() {
        Random random = new Random();
        String[] salespeople = {"Alice", "Bob", "Charlie", "Diana", "Eve"};
        String[] categories = {"Electronics", "Clothing", "Books", "Sports", "Home"};
        
        return IntStream.rangeClosed(1, 100)
            .mapToObj(i -> new SalesRecord(
                "2025-" + String.format("%02d", random.nextInt(12) + 1) + "-" + 
                String.format("%02d", random.nextInt(28) + 1),
                salespeople[random.nextInt(salespeople.length)],
                categories[random.nextInt(categories.length)],
                100 + random.nextDouble() * 2000
            ))
            .collect(Collectors.toList());
    }
    
    static class SalesRecord {
        private final String date;
        private final String salesperson;
        private final String category;
        private final double amount;
        
        public SalesRecord(String date, String salesperson, String category, double amount) {
            this.date = date;
            this.salesperson = salesperson;
            this.category = category;
            this.amount = amount;
        }
        
        public String getDate() { return date; }
        public String getSalesperson() { return salesperson; }
        public String getCategory() { return category; }
        public double getAmount() { return amount; }
        
        @Override
        public String toString() {
            return String.format("%s: %s - %s $%.2f", date, salesperson, category, amount);
        }
    }
}
```

---

## 🎯 Summary

**Congratulations!** You've completed **Day 37: Stream API Fundamentals** and mastered:

- **Stream Creation**: Multiple ways to create streams from various data sources
- **Intermediate Operations**: Filtering, mapping, sorting, and transforming data
- **Terminal Operations**: Collecting, reducing, and aggregating stream results
- **Collectors**: Powerful data collection and grouping techniques
- **Performance**: Understanding lazy evaluation, parallel streams, and optimization
- **Real-World Applications**: Log analysis, sales reporting, and data processing

**Key Takeaways:**
- Streams provide a functional approach to data processing with lazy evaluation
- Intermediate operations are chainable and don't execute until a terminal operation
- Collectors offer powerful aggregation and grouping capabilities
- Parallel streams can improve performance for CPU-intensive operations
- Stream pipelines make complex data transformations readable and maintainable

**Performance Tips:**
- Use primitive streams (IntStream, LongStream, DoubleStream) for better performance
- Consider parallel streams for large datasets and CPU-intensive operations
- Use `peek()` for debugging stream pipelines
- Avoid side effects in stream operations for predictable behavior

**Next Steps:** Tomorrow we'll explore **Advanced Stream Operations** including complex transformations, custom collectors, and parallel processing techniques.

**🏆 Day 37 Complete!** You now have solid fundamentals in the Stream API and can process data efficiently using functional programming techniques.