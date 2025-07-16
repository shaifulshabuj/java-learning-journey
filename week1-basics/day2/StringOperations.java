public class StringOperations {
    public static void main(String[] args) {
        System.out.println("String Operations Demo\n");

        // String declaration
        String greetings = "Hello";
        String name = "Java Developer";
        String message = greetings + ", " + name + "!";

        System.out.println("String Creation:");
        System.out.println("greetings: " + greetings);
        System.out.println("name: " + name);
        System.out.println("message: " + message + "\n");

        // String Properties
        System.out.println("\nString Properties:");
        System.out.println("Length of message: " + message.length());
        System.out.println("Is message empty? " + message.isEmpty());

        // String Methods
        System.out.println("\nString Methods:");
        System.out.println("Uppercase: " + message.toUpperCase());
        System.out.println("Lowercase: " + message.toLowerCase());
        System.out.println("First character: " + message.charAt(0));
        System.out.println("Last character: " + message.charAt(message.length() - 1));

        // String Searching
        System.out.println("\nString Searching:");
        String original = " Java Programming ";
        System.out.println("Original String: '" + original + "'");
        System.out.println("Trimmed String: '" + original.trim() + "'");
        System.out.println("Replaced: '" + original.replace("Java", "Python") + "'");

        // String manipulation
        System.out.println("\nString Manipulation:");
        String data = "Java,Python,C++,JavaScript";
        String[] languages = data.split(",");
        System.out.println("Original Data: " + data);
        System.out.println("Split into Array:");
        for (int i=0; i < languages.length; i++) {
            System.out.println("Language " + (i + 1) + ": " + languages[i]);
        }

        // String Comparison
        System.out.println("\nString Comparison:");
        String str1 = "Hello";
        String str2 = "hello";
        String str3 = "Hello";
        
        System.out.println("str1.equals(str2): " + str1.equals(str2));
        System.err.println("str1.equals(str3): " + str1.equals(str3));
        System.out.println("str1.equalsIgnoreCase(str2): " + str1.equalsIgnoreCase(str2));

        // String formatting
        System.out.println("\nString Formatting:");
        String template = "My name is %s, I am %d years old, and I earn $%.2f";
        String formatted = String.format(template, "Shabuj", 25, 50000.50);
        System.out.println(formatted);

    }
}