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
        System.out.println("Is Learning: " + isLearning);

        // 3. Simple calculation
        int firstNumber = 10;
        int secondNumber = 20;
        int sum = firstNumber + secondNumber;
        System.out.println("Sum of " + firstNumber + " and " + secondNumber + " is: " + sum);

        // 4. String operations
        String greeting = "Hello";
        String target = "Java";
        String message = greeting + " " + target + "!";

        System.out.println(message);
        System.out.println("Message length: " + message.length());
        System.out.println("Uppercase: " + message.toUpperCase());

    }
}