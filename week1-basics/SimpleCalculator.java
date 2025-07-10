public class SimpleCalculator {
    public static void main(String[] args) {
        double num1 = 15.5;
        double num2 = 5.5;
        int num1Int = (int) num1; // Casting double to int
        int num2Int = (int) num2; // Casting double to int
        String lineString = "-------------------";

        double sum = add(num1, num2);
        double difference = subtract(num1, num2);
        double product = multiply(num1, num2);
        double quotient = divide(num1, num2);
        double quotientZero = divide(num1, 0); // Example of division by zero

        System.out.println("Simple Calculator");
        System.out.println(lineString);
        System.out.println("Numbers: " + num1 + " and " + num2);
        System.out.println("Sum: " + sum);
        System.out.println("Difference: " + difference);
        System.out.println("Product: " + product);
        System.out.println("Quotient: " + quotient);
        System.out.println("Quotient (with zero): " + quotientZero);
        System.out.println(lineString);
        System.out.println("Casting to int");
        System.out.println("Casting to int: " + num1Int + " and " + num2Int);
        System.out.println("Sum of integers: " + (num1Int + num2Int));
        System.out.println("Difference of integers: " + (num1Int - num2Int));
        System.out.println("Product of integers: " + (num1Int * num2Int));
        System.out.println("Quotient of integers: " + (num1Int / num2Int));
        System.out.println(lineString);
        System.out.println("Casting back to double");
        System.out.println("Casting back to double: " + (double) num1Int + " and " + (double) num2Int);
        System.out.println("Sum of doubles from integers: " + ((double) num1Int + (double) num2Int));
        System.out.println("Difference of doubles from integers: " + ((double) num1Int - (double) num2Int));
        System.out.println("Product of doubles from integers: " + ((double) num1Int * (double) num2Int));
        System.out.println("Quotient of doubles from integers: " + ((double) num1Int / (double) num2Int));
        System.out.println(lineString);
        System.out.println("Thank you for using the Simple Calculator!");
    }

    public static double add(double a, double b) {
        return a + b;
    }

    public static double subtract(double a, double b) {
        return a - b;
    }

    public static double multiply(double a, double b) {
        return a * b;
    }

    public static double divide(double a, double b) {
        if (b != 0) {
            return a / b;
        } else {
            System.out.println("Error: Division by zero is not allowed.");
            return 0;
        }
    }
}