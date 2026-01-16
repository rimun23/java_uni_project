package perudo.ui;

import java.util.Scanner;

public final class ConsoleUI {
    private final Scanner sc = new Scanner(System.in);

    public void println(String s) {
        System.out.println(s);
    }

    public String readLine() {
        return sc.nextLine();
    }

    public int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            try {
                int v = Integer.parseInt(s);
                if (v < min || v > max) {
                    System.out.println("Enter a number in [" + min + ".." + max + "].");
                    continue;
                }
                return v;
            } catch (NumberFormatException e) {
                System.out.println("Not a number.");
            }
        }
    }

    public String readNonEmpty(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            if (!s.isEmpty()) return s;
            System.out.println("Empty. Try again.");
        }
    }
}
