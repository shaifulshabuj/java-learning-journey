public class OperatorsDemo {
    public static void main(String[] args) {
        System.out.println("Java Operators Demo");

        int a = 15, b = 4;

        // Arithmetic Operators
        System.out.println("Arithmetic Operators (a=15, b=4):");
        System.out.println("a + b = " + (a + b)); // Addition
        System.out.println("a - b = " + (a - b)); // Subtraction
        System.out.println("a * b = " + (a * b)); // Multiplication
        System.out.println("a / b = " + (a / b)); // Division (Integer)
        System.out.println("a % b = " + (a % b)); // Modulus (Remainder)

        // Division with double for decimal result
        double preciseDiv = (double) a / b; // Cast to double for decimal division
        System.out.println("a / b (precise) = " + preciseDiv);

        // Assignment Operators
        System.out.println("\nAssignment Operators:");
        int x = 10;
        System.out.println("Initial x = " + x);
        x += 5;
        System.out.println("After x += 5, x = " + x);
        x -= 3;
        System.out.println("After x -= 3, x = " + x);
        x *= 2;
        System.out.println("After x *= 2, x = " + x);
        x /= 4;
        System.out.println("After x /= 4, x = " + x);
        x %= 3;
        System.out.println("After x %= 3, x = " + x);

        // Increment and Decrement Operators
        System.out.println("\nIncrement and Decrement Operators:");
        int count = 5;
        System.out.println("Initial count = " + count);
        System.out.println("count++: " + count++); // Post-increment
        System.out.println("After post-increment, count = " + count);
        System.out.println("++count: " + ++count); // Pre-increment
        System.out.println("After pre-increment, count = " + count);
        System.out.println("count--: " + count--); // Post-decrement
        System.out.println("After post-decrement, count = " + count);
        System.out.println("--count: " + --count); // Pre-decrement
        System.out.println("After pre-decrement, count = " + count);

        // Comparison Operators
        System.out.println("\nComparison Operators: (a=15, b=4):");
        System.out.println("a == b: " + (a == b)); // Equal to
        System.out.println("a != b: " + (a != b)); // Not equal
        System.out.println("a > b: " + (a > b));   // Greater
        System.out.println("a < b: " + (a < b));   // Less
        System.out.println("a >= b: " + (a >= b)); // Greater than or equal
        System.out.println("a <= b: " + (a <= b)); // Less than or equal

        // Logical Operators
        System.out.println("\nLogical Operators:");
        boolean condition1 = true;
        boolean condition2 = false;

        System.out.println("condition1 && condition2: " + (condition1 && condition2)); // Logical AND
        System.out.println("condition1 || condition2: " + (condition1 || condition2)); // Logical OR
        System.out.println("!condition1: " + (!condition1)); // Logical NOT

        // Bitwise Operators
        System.out.println("\nBitwise Operators: (bitA=5(0101), bitB=3(0011))");
        int bitA = 5; // 0101 in binary
        int bitB = 3; // 0011 in binary
        System.out.println("bitA & bitB: " + (bitA & bitB)); // Bitwise AND
        System.out.println("bitA | bitB: " + (bitA | bitB)); // Bitwise OR
        System.out.println("bitA ^ bitB: " + (bitA ^ bitB)); // Bitwise XOR
        System.out.println("~bitA: " + (~bitA)); // Bitwise NOT
        System.out.println("bitA << 1: " + (bitA << 1)); // Left shift
        System.out.println("bitA >> 1: " + (bitA >> 1)); // Right shift
        System.out.println("bitA >>> 1: " + (bitA >>> 1)); // Unsigned right shift

        // Ternary Operator
        System.out.println("\nTernary Operator: (a=15, b=4)");
        int max = (a > b) ? a : b; // If a is greater
        System.out.println("Max of a and b [(a > b) ? a : b] = " + max);

        // Instanceof Operator
        System.out.println("\nInstanceof Operator: str = \"Hello, Java!\"");
        String str = "Hello, Java!";
        boolean isString = str instanceof String; // Check if str is an instance of String
        System.out.println("Is str an instance of String? " + isString);

        // Null check using instanceof
        System.out.println("\nNull Check using instanceof: obj = null");
        Object obj = null;
        boolean isNull = obj instanceof String; // Check if obj is an instance of String
        System.out.println("Is obj an instance of String? " + isNull);

        // Practical Example
        int age = 20;
        boolean hasLicense = true;
        boolean canDrive = (age >= 18 && hasLicense); // Check if eligible to drive
        System.out.println("\nPractical Example: Can drive (age >= 18 and has license)");
        System.out.println("Age: " + age + ", Has License: " + hasLicense);
        System.out.println("Can drive (age >= 18 and has license): " + canDrive);
        age = 17; // Change age to test condition
        canDrive = (age >= 18 && hasLicense); // Re-evaluate eligibility
        System.out.println("After changing age to 17, Can drive: " + canDrive);
        age = 20; // Reset age
        hasLicense = false; // Change license status
        canDrive = (age >= 18 && hasLicense); // Re-evaluate eligibility
        System.out.println("Age 20 but no license, Can drive: " + canDrive);
    }
}