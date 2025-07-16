# Day 13: File I/O and Serialization (July 22, 2025)

**Date:** July 22, 2025  
**Duration:** 2.5 hours  
**Focus:** Master File I/O operations, serialization, and modern NIO.2 for efficient file handling

---

## 🎯 Today's Learning Goals

By the end of this session, you'll:
- ✅ Master file and directory operations
- ✅ Read and write text files efficiently
- ✅ Handle binary files and data streams
- ✅ Implement object serialization and deserialization
- ✅ Use modern NIO.2 for advanced file operations
- ✅ Build a comprehensive file management system

---

## 📁 Part 1: File and Directory Operations (45 minutes)

### **Understanding File I/O in Java**

**File I/O Classes:**
- `File` - Represents files and directories (legacy)
- `FileInputStream/FileOutputStream` - For reading/writing bytes
- `FileReader/FileWriter` - For reading/writing characters
- `BufferedReader/BufferedWriter` - For efficient I/O operations
- `Path` and `Files` - Modern NIO.2 approach

### **File and Directory Operations**

Create `FileOperationsDemo.java`:

```java
import java.io.*;
import java.nio.file.*;
import java.util.Date;

/**
 * Comprehensive file and directory operations demonstration
 * Shows both traditional File class and modern NIO.2 approaches
 */
public class FileOperationsDemo {
    
    public static void main(String[] args) {
        System.out.println("=== File and Directory Operations Demo ===\n");
        
        // Basic file operations
        demonstrateBasicFileOperations();
        
        // Directory operations
        demonstrateDirectoryOperations();
        
        // File information
        demonstrateFileInformation();
        
        // Modern NIO.2 approach
        demonstrateNIOFileOperations();
    }
    
    private static void demonstrateBasicFileOperations() {
        System.out.println("1. Basic File Operations:");
        
        try {
            // Create a new file
            File file = new File("demo.txt");
            
            if (file.createNewFile()) {
                System.out.println("✅ File created: " + file.getName());
            } else {
                System.out.println("⚠️ File already exists: " + file.getName());
            }
            
            // Write to file
            FileWriter writer = new FileWriter(file);
            writer.write("Hello, File I/O!\n");
            writer.write("This is a test file.\n");
            writer.write("Learning Java File Operations.\n");
            writer.close();
            System.out.println("✅ Content written to file");
            
            // Read from file
            FileReader reader = new FileReader(file);
            int character;
            System.out.println("\n📖 File content:");
            while ((character = reader.read()) != -1) {
                System.out.print((char) character);
            }
            reader.close();
            
            // Delete file
            if (file.delete()) {
                System.out.println("\n🗑️ File deleted successfully");
            }
            
        } catch (IOException e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    private static void demonstrateDirectoryOperations() {
        System.out.println("2. Directory Operations:");
        
        try {
            // Create directory
            File directory = new File("test_directory");
            if (directory.mkdir()) {
                System.out.println("✅ Directory created: " + directory.getName());
            }
            
            // Create nested directories
            File nestedDir = new File("test_directory/nested/deep");
            if (nestedDir.mkdirs()) {
                System.out.println("✅ Nested directories created");
            }
            
            // Create files in directory
            File file1 = new File("test_directory/file1.txt");
            File file2 = new File("test_directory/file2.txt");
            File file3 = new File("test_directory/nested/file3.txt");
            
            file1.createNewFile();
            file2.createNewFile();
            file3.createNewFile();
            
            System.out.println("✅ Files created in directory");
            
            // List directory contents
            System.out.println("\n📁 Directory contents:");
            listDirectoryContents(directory, 0);
            
            // Clean up
            deleteDirectory(directory);
            System.out.println("\n🗑️ Directory and contents deleted");
            
        } catch (IOException e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    private static void listDirectoryContents(File directory, int level) {
        if (directory.isDirectory()) {
            String indent = "  ".repeat(level);
            File[] files = directory.listFiles();
            
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        System.out.println(indent + "📁 " + file.getName() + "/");
                        listDirectoryContents(file, level + 1);
                    } else {
                        System.out.println(indent + "📄 " + file.getName());
                    }
                }
            }
        }
    }
    
    private static void deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        directory.delete();
    }
    
    private static void demonstrateFileInformation() {
        System.out.println("3. File Information:");
        
        try {
            // Create a test file
            File testFile = new File("info_test.txt");
            FileWriter writer = new FileWriter(testFile);
            writer.write("This is a test file for information display.");
            writer.close();
            
            // Display file information
            System.out.println("File: " + testFile.getName());
            System.out.println("Absolute path: " + testFile.getAbsolutePath());
            System.out.println("Parent directory: " + testFile.getParent());
            System.out.println("Size: " + testFile.length() + " bytes");
            System.out.println("Last modified: " + new Date(testFile.lastModified()));
            System.out.println("Can read: " + testFile.canRead());
            System.out.println("Can write: " + testFile.canWrite());
            System.out.println("Can execute: " + testFile.canExecute());
            System.out.println("Is file: " + testFile.isFile());
            System.out.println("Is directory: " + testFile.isDirectory());
            System.out.println("Is hidden: " + testFile.isHidden());
            
            // Clean up
            testFile.delete();
            
        } catch (IOException e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    private static void demonstrateNIOFileOperations() {
        System.out.println("4. Modern NIO.2 File Operations:");
        
        try {
            // Create path
            Path path = Paths.get("nio_demo.txt");
            
            // Write to file using NIO.2
            String content = "Modern NIO.2 file operations\nFaster and more efficient!";
            Files.write(path, content.getBytes());
            System.out.println("✅ File written using NIO.2");
            
            // Read from file using NIO.2
            byte[] bytes = Files.readAllBytes(path);
            String readContent = new String(bytes);
            System.out.println("\n📖 File content read using NIO.2:");
            System.out.println(readContent);
            
            // File operations using NIO.2
            System.out.println("\n📊 File information using NIO.2:");
            System.out.println("Size: " + Files.size(path) + " bytes");
            System.out.println("Last modified: " + Files.getLastModifiedTime(path));
            System.out.println("Owner: " + Files.getOwner(path));
            System.out.println("Readable: " + Files.isReadable(path));
            System.out.println("Writable: " + Files.isWritable(path));
            System.out.println("Executable: " + Files.isExecutable(path));
            
            // Delete file
            Files.delete(path);
            System.out.println("\n🗑️ File deleted using NIO.2");
            
        } catch (IOException e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
        
        System.out.println();
    }
}
```

---

## 📝 Part 2: Text File Reading and Writing (45 minutes)

### **Efficient Text File Operations**

**Text I/O Classes:**
- `FileReader/FileWriter` - Basic character I/O
- `BufferedReader/BufferedWriter` - Buffered I/O for efficiency
- `Scanner` - Easy text parsing
- `PrintWriter` - Formatted text output

### **Comprehensive Text File Operations**

Create `TextFileOperationsDemo.java`:

```java
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Comprehensive text file operations demonstration
 * Shows different approaches to reading and writing text files
 */
public class TextFileOperationsDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Text File Operations Demo ===\n");
        
        // Basic text file operations
        demonstrateBasicTextIO();
        
        // Buffered text I/O
        demonstrateBufferedTextIO();
        
        // Scanner for text parsing
        demonstrateScannerOperations();
        
        // PrintWriter for formatted output
        demonstratePrintWriterOperations();
        
        // Modern NIO.2 text operations
        demonstrateNIOTextOperations();
    }
    
    private static void demonstrateBasicTextIO() {
        System.out.println("1. Basic Text File I/O:");
        
        String filename = "basic_text.txt";
        
        try {
            // Writing with FileWriter
            FileWriter writer = new FileWriter(filename);
            writer.write("Line 1: Hello World!\n");
            writer.write("Line 2: Java File I/O\n");
            writer.write("Line 3: Basic operations\n");
            writer.close();
            System.out.println("✅ Text written to file using FileWriter");
            
            // Reading with FileReader
            FileReader reader = new FileReader(filename);
            char[] buffer = new char[1024];
            int charsRead = reader.read(buffer);
            reader.close();
            
            System.out.println("\n📖 Content read using FileReader:");
            System.out.print(new String(buffer, 0, charsRead));
            
            // Clean up
            new File(filename).delete();
            
        } catch (IOException e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    private static void demonstrateBufferedTextIO() {
        System.out.println("2. Buffered Text I/O (More Efficient):");
        
        String filename = "buffered_text.txt";
        
        try {
            // Writing with BufferedWriter (more efficient)
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
            writer.write("Buffered writing is more efficient for multiple operations.\n");
            writer.write("It reduces the number of system calls.\n");
            writer.write("Perfect for writing large amounts of text.\n");
            writer.newLine(); // Platform-independent line separator
            writer.write("This is after a newline.");
            writer.close();
            System.out.println("✅ Text written using BufferedWriter");
            
            // Reading with BufferedReader (more efficient)
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String line;
            int lineNumber = 1;
            
            System.out.println("\n📖 Content read using BufferedReader:");
            while ((line = reader.readLine()) != null) {
                System.out.println(lineNumber + ": " + line);
                lineNumber++;
            }
            reader.close();
            
            // Clean up
            new File(filename).delete();
            
        } catch (IOException e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    private static void demonstrateScannerOperations() {
        System.out.println("3. Scanner for Text Parsing:");
        
        String filename = "scanner_demo.txt";
        
        try {
            // Create a file with structured data
            PrintWriter writer = new PrintWriter(filename);
            writer.println("John 25 Engineer 75000.50");
            writer.println("Alice 30 Doctor 120000.75");
            writer.println("Bob 28 Teacher 45000.25");
            writer.println("Carol 35 Lawyer 95000.80");
            writer.close();
            
            // Read structured data using Scanner
            Scanner scanner = new Scanner(new File(filename));
            
            System.out.println("\n📊 Parsing structured data with Scanner:");
            System.out.printf("%-10s %-5s %-10s %-10s%n", "Name", "Age", "Job", "Salary");
            System.out.println("-".repeat(40));
            
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                Scanner lineScanner = new Scanner(line);
                
                if (lineScanner.hasNext()) {
                    String name = lineScanner.next();
                    int age = lineScanner.nextInt();
                    String job = lineScanner.next();
                    double salary = lineScanner.nextDouble();
                    
                    System.out.printf("%-10s %-5d %-10s $%-9.2f%n", name, age, job, salary);
                }
                lineScanner.close();
            }
            scanner.close();
            
            // Parse CSV-like data
            System.out.println("\n📊 Parsing CSV-like data:");
            writer = new PrintWriter(filename);
            writer.println("Product,Price,Quantity,Category");
            writer.println("Laptop,999.99,50,Electronics");
            writer.println("Book,19.99,200,Education");
            writer.println("Coffee,4.50,100,Food");
            writer.close();
            
            scanner = new Scanner(new File(filename));
            
            // Skip header
            if (scanner.hasNextLine()) {
                System.out.println("Header: " + scanner.nextLine());
            }
            
            while (scanner.hasNextLine()) {
                String[] fields = scanner.nextLine().split(",");
                System.out.printf("Product: %s, Price: $%s, Qty: %s, Category: %s%n",
                                fields[0], fields[1], fields[2], fields[3]);
            }
            scanner.close();
            
            // Clean up
            new File(filename).delete();
            
        } catch (IOException e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    private static void demonstratePrintWriterOperations() {
        System.out.println("4. PrintWriter for Formatted Output:");
        
        String filename = "formatted_output.txt";
        
        try {
            PrintWriter writer = new PrintWriter(filename);
            
            // Formatted output
            writer.println("=== Sales Report ===");
            writer.println();
            writer.printf("%-15s %10s %10s %12s%n", "Product", "Price", "Quantity", "Total");
            writer.println("-".repeat(50));
            
            // Sample data
            Object[][] salesData = {
                {"Laptop", 999.99, 10, 9999.90},
                {"Mouse", 29.99, 50, 1499.50},
                {"Keyboard", 79.99, 25, 1999.75},
                {"Monitor", 299.99, 15, 4499.85}
            };
            
            double grandTotal = 0;
            
            for (Object[] row : salesData) {
                writer.printf("%-15s $%9.2f %10d $%11.2f%n", 
                            row[0], row[1], row[2], row[3]);
                grandTotal += (Double) row[3];
            }
            
            writer.println("-".repeat(50));
            writer.printf("%-37s $%11.2f%n", "Grand Total:", grandTotal);
            
            writer.close();
            System.out.println("✅ Formatted report written to file");
            
            // Read and display the formatted content
            System.out.println("\n📖 Formatted content:");
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            reader.close();
            
            // Clean up
            new File(filename).delete();
            
        } catch (IOException e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    private static void demonstrateNIOTextOperations() {
        System.out.println("5. Modern NIO.2 Text Operations:");
        
        Path path = Paths.get("nio_text.txt");
        
        try {
            // Write lines using NIO.2
            List<String> lines = Arrays.asList(
                "Modern NIO.2 text operations",
                "Easy to use and efficient",
                "Perfect for modern Java applications",
                "Supports various character encodings"
            );
            
            Files.write(path, lines, StandardCharsets.UTF_8);
            System.out.println("✅ Lines written using NIO.2");
            
            // Read all lines using NIO.2
            List<String> readLines = Files.readAllLines(path, StandardCharsets.UTF_8);
            System.out.println("\n📖 Lines read using NIO.2:");
            for (int i = 0; i < readLines.size(); i++) {
                System.out.println((i + 1) + ": " + readLines.get(i));
            }
            
            // Stream operations with files
            System.out.println("\n🔄 Stream operations with file content:");
            Files.lines(path)
                 .filter(line -> line.contains("NIO"))
                 .forEach(line -> System.out.println("Contains NIO: " + line));
            
            // Append to file
            Files.write(path, "\nAppended line using NIO.2".getBytes(), 
                       StandardOpenOption.APPEND);
            
            // Read entire file as string
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            System.out.println("\n📄 Complete file content:");
            System.out.println(content);
            
            // Clean up
            Files.delete(path);
            
        } catch (IOException e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
        
        System.out.println();
    }
}
```

---

## 🗂️ Part 3: Binary File Operations (30 minutes)

### **Binary File I/O**

**Binary I/O Classes:**
- `FileInputStream/FileOutputStream` - Basic byte I/O
- `BufferedInputStream/BufferedOutputStream` - Buffered byte I/O
- `DataInputStream/DataOutputStream` - Read/write primitive data types
- `ObjectInputStream/ObjectOutputStream` - Object serialization

### **Binary File Operations**

Create `BinaryFileOperationsDemo.java`:

```java
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Binary file operations demonstration
 * Shows how to handle binary data and primitive types
 */
public class BinaryFileOperationsDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Binary File Operations Demo ===\n");
        
        // Basic binary I/O
        demonstrateBasicBinaryIO();
        
        // Data streams for primitives
        demonstrateDataStreams();
        
        // Binary file copying
        demonstrateBinaryFileCopy();
        
        // Random access file operations
        demonstrateRandomAccessFile();
    }
    
    private static void demonstrateBasicBinaryIO() {
        System.out.println("1. Basic Binary File I/O:");
        
        String filename = "binary_demo.bin";
        
        try {
            // Write binary data
            FileOutputStream output = new FileOutputStream(filename);
            
            // Write some bytes
            byte[] data = "Hello Binary World!".getBytes();
            output.write(data);
            
            // Write individual bytes
            output.write(65); // 'A'
            output.write(66); // 'B'
            output.write(67); // 'C'
            
            output.close();
            System.out.println("✅ Binary data written to file");
            
            // Read binary data
            FileInputStream input = new FileInputStream(filename);
            
            System.out.println("\n📖 Reading binary data:");
            int byteValue;
            StringBuilder content = new StringBuilder();
            
            while ((byteValue = input.read()) != -1) {
                if (byteValue >= 32 && byteValue <= 126) { // Printable characters
                    content.append((char) byteValue);
                } else {
                    content.append("[" + byteValue + "]");
                }
            }
            
            input.close();
            System.out.println("Content: " + content.toString());
            
            // Clean up
            new File(filename).delete();
            
        } catch (IOException e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    private static void demonstrateDataStreams() {
        System.out.println("2. Data Streams for Primitive Types:");
        
        String filename = "data_types.dat";
        
        try {
            // Write primitive data types
            DataOutputStream output = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(filename))
            );
            
            // Write different data types
            output.writeInt(42);
            output.writeDouble(3.14159);
            output.writeBoolean(true);
            output.writeUTF("Hello DataStream!");
            output.writeLong(123456789L);
            output.writeFloat(2.71828f);
            output.writeChar('J');
            output.writeByte(127);
            
            output.close();
            System.out.println("✅ Primitive data types written to binary file");
            
            // Read primitive data types (must read in same order!)
            DataInputStream input = new DataInputStream(
                new BufferedInputStream(new FileInputStream(filename))
            );
            
            System.out.println("\n📖 Reading primitive data types:");
            System.out.println("int: " + input.readInt());
            System.out.println("double: " + input.readDouble());
            System.out.println("boolean: " + input.readBoolean());
            System.out.println("String: " + input.readUTF());
            System.out.println("long: " + input.readLong());
            System.out.println("float: " + input.readFloat());
            System.out.println("char: " + input.readChar());
            System.out.println("byte: " + input.readByte());
            
            input.close();
            
            // Clean up
            new File(filename).delete();
            
        } catch (IOException e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    private static void demonstrateBinaryFileCopy() {
        System.out.println("3. Binary File Copying:");
        
        String sourceFile = "source.bin";
        String destFile = "destination.bin";
        
        try {
            // Create source file with binary data
            FileOutputStream output = new FileOutputStream(sourceFile);
            Random random = new Random();
            
            // Write 1000 random bytes
            for (int i = 0; i < 1000; i++) {
                output.write(random.nextInt(256));
            }
            output.close();
            
            System.out.println("✅ Source file created with 1000 random bytes");
            
            // Copy file using buffered streams
            long startTime = System.nanoTime();
            
            BufferedInputStream input = new BufferedInputStream(new FileInputStream(sourceFile));
            BufferedOutputStream bufferedOutput = new BufferedOutputStream(new FileOutputStream(destFile));
            
            byte[] buffer = new byte[1024];
            int bytesRead;
            
            while ((bytesRead = input.read(buffer)) != -1) {
                bufferedOutput.write(buffer, 0, bytesRead);
            }
            
            input.close();
            bufferedOutput.close();
            
            long endTime = System.nanoTime();
            double duration = (endTime - startTime) / 1_000_000.0; // Convert to milliseconds
            
            System.out.printf("✅ File copied in %.2f ms%n", duration);
            
            // Verify copy
            File source = new File(sourceFile);
            File dest = new File(destFile);
            
            System.out.println("Source size: " + source.length() + " bytes");
            System.out.println("Destination size: " + dest.length() + " bytes");
            System.out.println("Copy successful: " + (source.length() == dest.length()));
            
            // Clean up
            source.delete();
            dest.delete();
            
        } catch (IOException e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    private static void demonstrateRandomAccessFile() {
        System.out.println("4. Random Access File Operations:");
        
        String filename = "random_access.dat";
        
        try {
            // Create file with random access
            RandomAccessFile raf = new RandomAccessFile(filename, "rw");
            
            // Write data at specific positions
            raf.writeInt(100);        // Position 0-3
            raf.writeDouble(3.14159); // Position 4-11
            raf.writeUTF("Hello");   // Position 12+
            
            System.out.println("✅ Data written to random access file");
            System.out.println("File length: " + raf.length() + " bytes");
            
            // Read data from specific positions
            System.out.println("\n📖 Reading from specific positions:");
            
            // Read from beginning
            raf.seek(0);
            System.out.println("Integer at position 0: " + raf.readInt());
            
            // Read from position 4
            raf.seek(4);
            System.out.println("Double at position 4: " + raf.readDouble());
            
            // Read from position 12
            raf.seek(12);
            System.out.println("String at position 12: " + raf.readUTF());
            
            // Modify data at specific position
            raf.seek(0);
            raf.writeInt(999); // Change first integer
            
            // Verify modification
            raf.seek(0);
            System.out.println("Modified integer: " + raf.readInt());
            
            raf.close();
            
            // Clean up
            new File(filename).delete();
            
        } catch (IOException e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
        
        System.out.println();
    }
}
```

---

## 🎯 Part 4: Object Serialization (30 minutes)

### **Object Serialization**

**Serialization Concepts:**
- **Serialization:** Converting objects to byte streams
- **Deserialization:** Converting byte streams back to objects
- **Serializable Interface:** Marker interface for serializable classes
- **transient keyword:** Exclude fields from serialization
- **serialVersionUID:** Version control for serialized classes

### **Serialization Examples**

Create `SerializationDemo.java`:

```java
import java.io.*;
import java.util.*;

/**
 * Object serialization demonstration
 * Shows how to serialize and deserialize objects
 */
public class SerializationDemo {
    
    // Serializable Student class
    static class Student implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String name;
        private int age;
        private double gpa;
        private List<String> courses;
        private transient String password; // Will not be serialized
        
        public Student(String name, int age, double gpa) {
            this.name = name;
            this.age = age;
            this.gpa = gpa;
            this.courses = new ArrayList<>();
            this.password = "secret123"; // Transient field
        }
        
        public void addCourse(String course) {
            courses.add(course);
        }
        
        @Override
        public String toString() {
            return String.format("Student[name=%s, age=%d, gpa=%.2f, courses=%s, password=%s]",
                               name, age, gpa, courses, password);
        }
        
        // Getters
        public String getName() { return name; }
        public int getAge() { return age; }
        public double getGpa() { return gpa; }
        public List<String> getCourses() { return courses; }
        public String getPassword() { return password; }
    }
    
    // Serializable Course class with custom serialization
    static class Course implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String courseName;
        private String instructor;
        private int credits;
        private Date startDate;
        
        public Course(String courseName, String instructor, int credits) {
            this.courseName = courseName;
            this.instructor = instructor;
            this.credits = credits;
            this.startDate = new Date();
        }
        
        // Custom serialization method
        private void writeObject(ObjectOutputStream out) throws IOException {
            System.out.println("📝 Custom serialization for: " + courseName);
            out.defaultWriteObject();
        }
        
        // Custom deserialization method
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            System.out.println("📖 Custom deserialization for: " + courseName);
        }
        
        @Override
        public String toString() {
            return String.format("Course[name=%s, instructor=%s, credits=%d, startDate=%s]",
                               courseName, instructor, credits, startDate);
        }
    }
    
    public static void main(String[] args) {
        System.out.println("=== Object Serialization Demo ===\n");
        
        // Basic object serialization
        demonstrateBasicSerialization();
        
        // Collection serialization
        demonstrateCollectionSerialization();
        
        // Custom serialization
        demonstrateCustomSerialization();
        
        // Serialization best practices
        demonstrateBestPractices();
    }
    
    private static void demonstrateBasicSerialization() {
        System.out.println("1. Basic Object Serialization:");
        
        String filename = "student.ser";
        
        try {
            // Create student object
            Student student = new Student("John Doe", 20, 3.75);
            student.addCourse("Java Programming");
            student.addCourse("Data Structures");
            student.addCourse("Database Systems");
            
            System.out.println("Original student: " + student);
            
            // Serialize object
            ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(filename));
            output.writeObject(student);
            output.close();
            
            System.out.println("✅ Student object serialized to file");
            
            // Deserialize object
            ObjectInputStream input = new ObjectInputStream(new FileInputStream(filename));
            Student deserializedStudent = (Student) input.readObject();
            input.close();
            
            System.out.println("📖 Deserialized student: " + deserializedStudent);
            System.out.println("⚠️ Note: password field is null (transient)");
            
            // Clean up
            new File(filename).delete();
            
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    private static void demonstrateCollectionSerialization() {
        System.out.println("2. Collection Serialization:");
        
        String filename = "students.ser";
        
        try {
            // Create list of students
            List<Student> students = new ArrayList<>();
            students.add(new Student("Alice", 19, 3.9));
            students.add(new Student("Bob", 21, 3.2));
            students.add(new Student("Carol", 20, 3.7));
            
            // Add courses to students
            students.get(0).addCourse("Mathematics");
            students.get(0).addCourse("Physics");
            students.get(1).addCourse("Chemistry");
            students.get(2).addCourse("Biology");
            students.get(2).addCourse("English");
            
            System.out.println("Original student list size: " + students.size());
            
            // Serialize collection
            ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(filename));
            output.writeObject(students);
            output.close();
            
            System.out.println("✅ Student list serialized to file");
            
            // Deserialize collection
            ObjectInputStream input = new ObjectInputStream(new FileInputStream(filename));
            @SuppressWarnings("unchecked")
            List<Student> deserializedStudents = (List<Student>) input.readObject();
            input.close();
            
            System.out.println("📖 Deserialized student list:");
            for (int i = 0; i < deserializedStudents.size(); i++) {
                System.out.println("  " + (i + 1) + ". " + deserializedStudents.get(i));
            }
            
            // Clean up
            new File(filename).delete();
            
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    private static void demonstrateCustomSerialization() {
        System.out.println("3. Custom Serialization:");
        
        String filename = "course.ser";
        
        try {
            // Create course object
            Course course = new Course("Advanced Java", "Dr. Smith", 3);
            
            System.out.println("Original course: " + course);
            
            // Serialize with custom logic
            ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(filename));
            output.writeObject(course);
            output.close();
            
            System.out.println("✅ Course object serialized with custom logic");
            
            // Deserialize with custom logic
            ObjectInputStream input = new ObjectInputStream(new FileInputStream(filename));
            Course deserializedCourse = (Course) input.readObject();
            input.close();
            
            System.out.println("📖 Deserialized course: " + deserializedCourse);
            
            // Clean up
            new File(filename).delete();
            
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    private static void demonstrateBestPractices() {
        System.out.println("4. Serialization Best Practices:");
        
        String filename = "best_practices.ser";
        
        try {
            // Create complex object structure
            Map<String, Student> studentMap = new HashMap<>();
            
            Student student1 = new Student("David", 22, 3.8);
            student1.addCourse("Software Engineering");
            student1.addCourse("Computer Networks");
            
            Student student2 = new Student("Emma", 21, 3.9);
            student2.addCourse("Machine Learning");
            student2.addCourse("Artificial Intelligence");
            
            studentMap.put("student1", student1);
            studentMap.put("student2", student2);
            
            System.out.println("Original map size: " + studentMap.size());
            
            // Serialize map
            ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(filename));
            output.writeObject(studentMap);
            output.close();
            
            System.out.println("✅ Student map serialized");
            
            // Deserialize map
            ObjectInputStream input = new ObjectInputStream(new FileInputStream(filename));
            @SuppressWarnings("unchecked")
            Map<String, Student> deserializedMap = (Map<String, Student>) input.readObject();
            input.close();
            
            System.out.println("📖 Deserialized map:");
            for (Map.Entry<String, Student> entry : deserializedMap.entrySet()) {
                System.out.println("  " + entry.getKey() + ": " + entry.getValue());
            }
            
            // Demonstrate serialization with versioning
            System.out.println("\n📋 Serialization Best Practices:");
            System.out.println("✅ Always implement serialVersionUID");
            System.out.println("✅ Use transient for sensitive data");
            System.out.println("✅ Consider custom serialization for complex objects");
            System.out.println("✅ Handle ClassNotFoundException and IOException");
            System.out.println("✅ Close streams properly (try-with-resources)");
            
            // Clean up
            new File(filename).delete();
            
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
        
        System.out.println();
    }
}
```

---

## 🏗️ Part 5: File Management System Project (30 minutes)

### **Comprehensive File Management System**

Create `FileManagementSystem.java`:

```java
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Comprehensive File Management System
 * Demonstrates practical file I/O operations in a real-world scenario
 */
public class FileManagementSystem {
    
    // File metadata class
    static class FileInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String fileName;
        private String filePath;
        private long size;
        private LocalDateTime lastModified;
        private String fileType;
        private boolean isDirectory;
        
        public FileInfo(String fileName, String filePath, long size, 
                       LocalDateTime lastModified, String fileType, boolean isDirectory) {
            this.fileName = fileName;
            this.filePath = filePath;
            this.size = size;
            this.lastModified = lastModified;
            this.fileType = fileType;
            this.isDirectory = isDirectory;
        }
        
        // Getters
        public String getFileName() { return fileName; }
        public String getFilePath() { return filePath; }
        public long getSize() { return size; }
        public LocalDateTime getLastModified() { return lastModified; }
        public String getFileType() { return fileType; }
        public boolean isDirectory() { return isDirectory; }
        
        @Override
        public String toString() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return String.format("%-20s %-10s %10s %s %s",
                               fileName,
                               isDirectory ? "<DIR>" : fileType,
                               isDirectory ? "" : formatSize(size),
                               lastModified.format(formatter),
                               filePath);
        }
        
        private String formatSize(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
            if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
    
    private List<FileInfo> fileIndex;
    private String indexFilePath;
    
    public FileManagementSystem() {
        this.fileIndex = new ArrayList<>();
        this.indexFilePath = "file_index.ser";
        loadIndex();
    }
    
    public static void main(String[] args) {
        System.out.println("=== File Management System Demo ===\n");
        
        FileManagementSystem fms = new FileManagementSystem();
        
        // Create sample directory structure
        fms.createSampleStructure();
        
        // Scan and index files
        fms.scanDirectory("sample_project");
        
        // Display file listing
        fms.displayFiles();
        
        // Search operations
        fms.searchFiles("*.java");
        fms.searchFiles("*.txt");
        
        // File operations
        fms.copyFile("sample_project/src/Main.java", "sample_project/backup/Main_backup.java");
        
        // Generate report
        fms.generateReport();
        
        // Save index
        fms.saveIndex();
        
        // Cleanup
        fms.cleanup();
    }
    
    private void createSampleStructure() {
        System.out.println("📁 Creating sample directory structure:");
        
        try {
            // Create directory structure
            String[] directories = {
                "sample_project",
                "sample_project/src",
                "sample_project/lib",
                "sample_project/docs",
                "sample_project/backup"
            };
            
            for (String dir : directories) {
                Files.createDirectories(Paths.get(dir));
            }
            
            // Create sample files
            Map<String, String> files = new HashMap<>();
            files.put("sample_project/README.md", "# Sample Project\nThis is a sample project for file management demo.");
            files.put("sample_project/src/Main.java", "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello World!\");\n    }\n}");
            files.put("sample_project/src/Utils.java", "public class Utils {\n    public static void log(String message) {\n        System.out.println(message);\n    }\n}");
            files.put("sample_project/docs/manual.txt", "User Manual\n==========\n\nThis is the user manual for the sample project.");
            files.put("sample_project/lib/library.jar", "dummy jar file content");
            
            for (Map.Entry<String, String> entry : files.entrySet()) {
                Files.write(Paths.get(entry.getKey()), entry.getValue().getBytes());
            }
            
            System.out.println("✅ Sample structure created successfully\n");
            
        } catch (IOException e) {
            System.err.println("❌ Error creating sample structure: " + e.getMessage());
        }
    }
    
    private void scanDirectory(String directoryPath) {
        System.out.println("🔍 Scanning directory: " + directoryPath);
        
        try {
            Path startPath = Paths.get(directoryPath);
            
            Files.walk(startPath)
                 .forEach(path -> {
                     try {
                         BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                         
                         String fileName = path.getFileName().toString();
                         String filePath = path.toString();
                         long size = attrs.size();
                         LocalDateTime lastModified = LocalDateTime.ofInstant(
                             attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault());
                         boolean isDirectory = attrs.isDirectory();
                         String fileType = isDirectory ? "DIR" : getFileExtension(fileName);
                         
                         FileInfo fileInfo = new FileInfo(fileName, filePath, size, 
                                                          lastModified, fileType, isDirectory);
                         fileIndex.add(fileInfo);
                         
                     } catch (IOException e) {
                         System.err.println("❌ Error reading file attributes: " + e.getMessage());
                     }
                 });
            
            System.out.println("✅ Scanned " + fileIndex.size() + " items\n");
            
        } catch (IOException e) {
            System.err.println("❌ Error scanning directory: " + e.getMessage());
        }
    }
    
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex) : "";
    }
    
    private void displayFiles() {
        System.out.println("📋 File Listing:");
        System.out.println("=" .repeat(80));
        System.out.printf("%-20s %-10s %10s %19s %s%n", "Name", "Type", "Size", "Modified", "Path");
        System.out.println("-".repeat(80));
        
        fileIndex.stream()
                 .sorted(Comparator.comparing(FileInfo::getFilePath))
                 .forEach(System.out::println);
        
        System.out.println("-".repeat(80));
        System.out.println("Total items: " + fileIndex.size());
        System.out.println();
    }
    
    private void searchFiles(String pattern) {
        System.out.println("🔎 Searching for files matching pattern: " + pattern);
        
        // Convert glob pattern to regex
        String regex = pattern.replace("*", ".*").replace("?", ".");
        
        List<FileInfo> matches = fileIndex.stream()
                                          .filter(file -> !file.isDirectory())
                                          .filter(file -> file.getFileName().matches(regex))
                                          .collect(Collectors.toList());
        
        if (matches.isEmpty()) {
            System.out.println("❌ No files found matching pattern: " + pattern);
        } else {
            System.out.println("✅ Found " + matches.size() + " files matching pattern:");
            matches.forEach(file -> System.out.println("  " + file.getFileName() + " (" + file.getFilePath() + ")"));
        }
        
        System.out.println();
    }
    
    private void copyFile(String sourcePath, String destinationPath) {
        System.out.println("📋 Copying file: " + sourcePath + " → " + destinationPath);
        
        try {
            Path source = Paths.get(sourcePath);
            Path destination = Paths.get(destinationPath);
            
            // Create parent directories if they don't exist
            Files.createDirectories(destination.getParent());
            
            // Copy file
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
            
            System.out.println("✅ File copied successfully");
            
            // Update index with new file
            BasicFileAttributes attrs = Files.readAttributes(destination, BasicFileAttributes.class);
            String fileName = destination.getFileName().toString();
            long size = attrs.size();
            LocalDateTime lastModified = LocalDateTime.ofInstant(
                attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault());
            String fileType = getFileExtension(fileName);
            
            FileInfo newFileInfo = new FileInfo(fileName, destinationPath, size, 
                                               lastModified, fileType, false);
            fileIndex.add(newFileInfo);
            
        } catch (IOException e) {
            System.err.println("❌ Error copying file: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    private void generateReport() {
        System.out.println("📊 Generating file system report:");
        
        try {
            PrintWriter writer = new PrintWriter("file_report.txt");
            
            writer.println("FILE SYSTEM REPORT");
            writer.println("==================");
            writer.println("Generated: " + LocalDateTime.now());
            writer.println();
            
            // Summary statistics
            long totalFiles = fileIndex.stream().filter(f -> !f.isDirectory()).count();
            long totalDirectories = fileIndex.stream().filter(FileInfo::isDirectory).count();
            long totalSize = fileIndex.stream().filter(f -> !f.isDirectory()).mapToLong(FileInfo::getSize).sum();
            
            writer.println("SUMMARY");
            writer.println("-------");
            writer.printf("Total Files: %d%n", totalFiles);
            writer.printf("Total Directories: %d%n", totalDirectories);
            writer.printf("Total Size: %s%n", formatSize(totalSize));
            writer.println();
            
            // File types statistics
            Map<String, Long> fileTypes = fileIndex.stream()
                                                   .filter(f -> !f.isDirectory())
                                                   .collect(Collectors.groupingBy(
                                                       FileInfo::getFileType,
                                                       Collectors.counting()));
            
            writer.println("FILE TYPES");
            writer.println("----------");
            fileTypes.entrySet().stream()
                     .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                     .forEach(entry -> writer.printf("%-10s: %d files%n", entry.getKey(), entry.getValue()));
            writer.println();
            
            // Detailed file listing
            writer.println("DETAILED LISTING");
            writer.println("----------------");
            writer.printf("%-20s %-10s %10s %19s %s%n", "Name", "Type", "Size", "Modified", "Path");
            writer.println("-".repeat(80));
            
            fileIndex.stream()
                     .sorted(Comparator.comparing(FileInfo::getFilePath))
                     .forEach(writer::println);
            
            writer.close();
            
            System.out.println("✅ Report generated: file_report.txt");
            
        } catch (IOException e) {
            System.err.println("❌ Error generating report: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
    
    private void saveIndex() {
        System.out.println("💾 Saving file index:");
        
        try {
            ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(indexFilePath));
            output.writeObject(fileIndex);
            output.close();
            
            System.out.println("✅ File index saved successfully");
            
        } catch (IOException e) {
            System.err.println("❌ Error saving index: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    private void loadIndex() {
        try {
            if (Files.exists(Paths.get(indexFilePath))) {
                ObjectInputStream input = new ObjectInputStream(new FileInputStream(indexFilePath));
                @SuppressWarnings("unchecked")
                List<FileInfo> loadedIndex = (List<FileInfo>) input.readObject();
                this.fileIndex = loadedIndex;
                input.close();
                
                System.out.println("✅ File index loaded: " + fileIndex.size() + " items");
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("ℹ️ No existing index found, starting fresh");
            this.fileIndex = new ArrayList<>();
        }
    }
    
    private void cleanup() {
        System.out.println("🧹 Cleaning up sample files:");
        
        try {
            // Delete sample directory
            Path sampleDir = Paths.get("sample_project");
            if (Files.exists(sampleDir)) {
                Files.walk(sampleDir)
                     .sorted(Comparator.reverseOrder())
                     .map(Path::toFile)
                     .forEach(File::delete);
            }
            
            // Delete report and index files
            Files.deleteIfExists(Paths.get("file_report.txt"));
            Files.deleteIfExists(Paths.get(indexFilePath));
            
            System.out.println("✅ Cleanup completed");
            
        } catch (IOException e) {
            System.err.println("❌ Error during cleanup: " + e.getMessage());
        }
    }
}
```

---

## 📝 Key Takeaways from Day 13

### **File I/O Mastery:**
- ✅ **File Operations:** Create, read, write, delete files and directories
- ✅ **Text Files:** Handle character-based file operations efficiently
- ✅ **Binary Files:** Work with byte-based file operations and data streams
- ✅ **Serialization:** Convert objects to/from byte streams for persistence
- ✅ **NIO.2:** Modern file operations with better performance and features

### **Best Practices:**
- ✅ **Resource Management:** Always close streams and use try-with-resources
- ✅ **Error Handling:** Proper exception handling for I/O operations
- ✅ **Efficiency:** Use buffered streams for better performance
- ✅ **Portability:** Use Path and Files classes for cross-platform compatibility
- ✅ **Security:** Be cautious with file permissions and sensitive data

### **Advanced Features:**
- ✅ **Random Access:** Use RandomAccessFile for non-sequential file access
- ✅ **File Attributes:** Read and modify file metadata
- ✅ **Directory Walking:** Traverse directory structures efficiently
- ✅ **File Watching:** Monitor file system changes (NIO.2)
- ✅ **Memory Mapping:** Use memory-mapped files for large files

---

## 🎯 Today's Success Criteria

**Completed Tasks:**
- [ ] Master file and directory operations with both legacy and modern APIs
- [ ] Implement efficient text file reading and writing with various approaches
- [ ] Handle binary file operations and primitive data types
- [ ] Create serializable classes and demonstrate object serialization
- [ ] Use NIO.2 for advanced file operations and better performance
- [ ] Build a comprehensive file management system with indexing and search

**Bonus Achievements:**
- [ ] Implement file compression and decompression
- [ ] Create a file synchronization utility
- [ ] Build a log file analyzer
- [ ] Implement file encryption and decryption
- [ ] Create a backup and restore system

---

## 🚀 Tomorrow's Preview (Day 14 - July 23)

**Focus:** Lambda Expressions and Stream API
- Functional interfaces and lambda expressions
- Stream API for data processing and transformation
- Method references and functional programming concepts
- Building modern Java applications with functional style
- Parallel streams and performance optimization

---

## 🎉 Excellent Progress!

You've mastered file I/O and serialization! These skills are essential for data persistence, file management, and building robust Java applications. Tomorrow we'll explore modern functional programming with lambdas and streams.

**Keep building with Java!** 🚀