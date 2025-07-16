public class DataTypeConverter {
    public static void main(String[] args) {
        System.out.println("Data Type Conversion Demo\n");

        // Temperature Conversions
        double celsius = 25.0;
        double fahrenheit = (celsius * 9.0 / 5.0) + 32;
        double kelvin = celsius + 273.15;

        System.out.println("Temperature Conversions:");
        System.out.printf("%.1f°C = %.1f°F = %.2fK%n", celsius, fahrenheit, kelvin);

        // Distance Conversions
        double kilometers = 10.5;
        double miles = kilometers * 0.621371;
        double meters = kilometers * 1000;
        double feet = kilometers * 3280.84;

        System.out.println("\nDistance Conversions:");
        System.out.printf("%.2f km = %.2f miles = %.2f meters = %.2f feet%n", kilometers, miles, meters, feet);

        // Weight Conversions
        double kilograms = 70.0;
        double pounds = kilograms * 2.20462;
        double grams = kilograms * 1000;
        double ounces = pounds * 16;

        System.out.println("\nWeight Conversions:");
        System.out.printf("%.1f kg = %.1f lbs = %.0f grams = %.0f ounces%n", kilograms, pounds, grams, ounces);

    }
}