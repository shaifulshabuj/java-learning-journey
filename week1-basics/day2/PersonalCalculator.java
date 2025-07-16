public class PersonalCalculator {
    public static void main(String[] args){
        System.out.println("Personal Financial Calculator\n");

        // Your financial data
        final double MONTHLY_SALARY = 75000.00;
        final int WORKING_DAYS_PER_MONTH = 22;
        final double TAX_RATE = 0.15; // 15% tax rate

        // Calculate daily and hourly rates
        double dailySalary = MONTHLY_SALARY / WORKING_DAYS_PER_MONTH;
        double hourlySalary = dailySalary / 8;

        // Calculate monthly tax and net salary
        double monthlyTax = MONTHLY_SALARY * TAX_RATE;
        double netMonthlySalary = MONTHLY_SALARY - monthlyTax;
        double annualSalary = MONTHLY_SALARY * 12;
        double netAnnualSalary = netMonthlySalary * 12;

        // Display results
        System.out.println("Salary Breakdown:");
        System.out.printf("Monthly Gross Salary: $%.2f%n", MONTHLY_SALARY);
        System.out.printf("Daily Salary: $%.2f%n", dailySalary);
        System.out.printf("Hourly Salary: $%.2f%n", hourlySalary);
        System.out.println();

        System.out.println("Tax Information:");
        System.out.printf("Tax Rate: %.1f%%%n", TAX_RATE * 100);
        System.out.printf("Monthly Tax: $%.2f%n", monthlyTax);
        System.out.printf("Net Monthly Salary: $%.2f%n", netMonthlySalary);
        System.out.println();

        System.out.println("Annual Salary Information:");
        System.out.printf("Annual Gross Salary: $%.2f%n", annualSalary);
        System.out.printf("Net Annual Salary: $%.2f%n", netAnnualSalary);
        System.out.println();

        // Bonus : Calculate savings
        double monthlyExpenses = 30000.00; // Example monthly expenses
        double monthlySavings = netMonthlySalary - monthlyExpenses;
        double annualSavings = monthlySavings * 12;

        System.out.println("Savings Information:");
        System.out.printf("Monthly Expenses: $%.2f%n", monthlyExpenses);
        System.out.printf("Monthly Savings: $%.2f%n", monthlySavings);
        System.out.printf("Annual Savings: $%.2f%n", annualSavings);

        if (monthlySavings < 0) {
            System.out.println("Warning: You are spending more than you earn!");
        } else {
            System.out.println("Great! You are saving money each month.");
        }
    }
}