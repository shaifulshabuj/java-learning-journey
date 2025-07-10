public class DataTypesDemo {
    public static void main(String[] args) {
        System.out.println("Java Data Types Demo");

        // Integer data type
        byte smallNumber = 127; // 1 byte
        short mediumNumber = 32000; // 2 bytes
        int regularNumber = 2000000; // 4 bytes
        long bigNumber = 9000000000L; // 8 bytes ('L' suffix for long)

        System.out.println("Integer Types:");
        System.out.println("Byte: " + smallNumber);
        System.out.println("Short: " + mediumNumber);
        System.out.println("Int: " + regularNumber);
        System.out.println("Long: " + bigNumber);

        // Floating-point data types
        float price = 199.99f; // 4 bytes ('f' suffix for float)
        double preciseValue = 3.141592653589793; // 8 bytes

        System.out.println("\nFloating-point Types:");
        System.out.println("Float: " + price);
        System.out.println("Double: " + preciseValue);

        // Character data type
        char letterGrade = 'A'; // 2 bytes (Unicode character)
        boolean isPassingGrade = true; // 1 byte (true/false)

        System.err.println("\nCharacter and Boolean Types:");
        System.err.println("Char: " + letterGrade);
        System.err.println("Boolean: " + isPassingGrade);

        // Constant data type
        final int DAYS_IN_WEEK = 7; // Constant variable (final)
        final String COMPANY = "JavaTech"; // Constant string

        System.err.println("\nConstants:");
        System.err.println("Days in Week: " + DAYS_IN_WEEK);
        System.err.println("Company: " + COMPANY);

        // Size demontration
        System.out.println("\nType Sizes (in bytes):");
        System.err.println("byte: " + Byte.BYTES);
        System.err.println("short: " + Short.BYTES);
        System.err.println("int: " + Integer.BYTES);
        System.err.println("long: " + Long.BYTES);
        System.err.println("float: " + Float.BYTES);
        System.err.println("double: " + Double.BYTES);
        System.err.println("char: " + Character.BYTES);

    }
}