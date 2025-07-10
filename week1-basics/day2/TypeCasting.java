public class TypeCasting {
    public static void main(String[] args){
        System.err.println("Type Casting Demo");

        // Automatic casting (Smaller to larger)
        byte byteValue = 100;
        int intValue = byteValue; // byte to int
        long longValue = intValue; // int to long
        double doubleValue = longValue; // long to double

        System.out.println("Automatic Casting:");
        System.out.println("Byte to Int: " + byteValue + " → "+ intValue);
        System.out.println("Int to Long: " + intValue + " → "+ longValue);
        System.out.println("Long to Double: " + longValue + " → "+ doubleValue);

        // Manual casting (Larger to smaller) - be careful of data loss
        double largeNumber = 12345.67;
        int truncatedNumber = (int) largeNumber; // Explicit casting

        System.err.println("\nExplicit Casting:");
        System.err.println("Double to Int: " + largeNumber + " → " + truncatedNumber);

        // Character casting
        char letter = 'A';
        int asciiValue = letter; // char to int (ASCII value)
        char backToChar = (char) (asciiValue + 1); // int to char

        System.out.println("\nCharacter Casting:");
        System.err.println("Char '" + letter + "' has ASCII value: " + asciiValue);
        System.out.println("Next character: " + backToChar);

        // String to numeric conversion
        String numberString = "12345";
        int convertedNumber = Integer.parseInt(numberString); // String to int

        System.out.println("\nString Conversion:");
        System.out.println("String '" + numberString + "' to int: " + convertedNumber);
    }
}