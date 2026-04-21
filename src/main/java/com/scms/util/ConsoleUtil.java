package com.scms.util;

import java.util.Scanner;

/**
 * ConsoleUtil - Helper class for clean console I/O.
 *
 * Provides:
 *  - Pretty table printing using ResultSetMetaData
 *  - Section headers / dividers
 *  - Safe input reading
 */
public class ConsoleUtil {

    // Shared Scanner for the entire application (stdin)
    private static final Scanner SCANNER = new Scanner(System.in);

    // ─── Formatting constants ──────────────────────────────────────

    public static final String RESET  = "\u001B[0m";
    public static final String BOLD   = "\u001B[1m";
    public static final String CYAN   = "\u001B[36m";
    public static final String GREEN  = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String RED    = "\u001B[31m";
    public static final String BLUE   = "\u001B[34m";

    // ─── Headers & Dividers ───────────────────────────────────────

    public static void printHeader(String title) {
        System.out.println();
        System.out.println(CYAN + "╔══════════════════════════════════════════════╗" + RESET);
        System.out.printf( CYAN + "║  %-44s║%n" + RESET, title);
        System.out.println(CYAN + "╚══════════════════════════════════════════════╝" + RESET);
    }

    public static void printDivider() {
        System.out.println("──────────────────────────────────────────────────");
    }

    public static void printSuccess(String message) {
        System.out.println(GREEN + "✔  " + message + RESET);
    }

    public static void printError(String message) {
        System.out.println(RED + "✘  " + message + RESET);
    }

    public static void printInfo(String message) {
        System.out.println(YELLOW + "ℹ  " + message + RESET);
    }

    // ─── Input helpers ────────────────────────────────────────────

    public static String prompt(String label) {
        System.out.print(BOLD + label + ": " + RESET);
        return SCANNER.nextLine().trim();
    }

    public static int promptInt(String label) {
        while (true) {
            System.out.print(BOLD + label + ": " + RESET);
            String input = SCANNER.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                printError("Please enter a valid number.");
            }
        }
    }

    public static String promptMenuChoice(String... options) {
        for (int i = 0; i < options.length; i++) {
            System.out.printf("  %s[%d]%s %s%n", BOLD, i + 1, RESET, options[i]);
        }
        System.out.print(BOLD + "Choice: " + RESET);
        return SCANNER.nextLine().trim();
    }

    public static void waitForEnter() {
        System.out.print(YELLOW + "\n  [Press ENTER to continue...]" + RESET);
        SCANNER.nextLine();
    }

    public static Scanner getScanner() {
        return SCANNER;
    }

    // ─── Dynamic table printer using raw string arrays ────────────

    /**
     * Prints a formatted ASCII table from a 2D string array.
     *
     * @param headers Column headers
     * @param rows    Table data (each row is a String[])
     */
    public static void printTable(String[] headers, java.util.List<String[]> rows) {
        if (headers == null || headers.length == 0) return;

        // Calculate column widths
        int[] widths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            widths[i] = headers[i].length();
        }
        for (String[] row : rows) {
            for (int i = 0; i < Math.min(row.length, widths.length); i++) {
                if (row[i] != null && row[i].length() > widths[i]) {
                    widths[i] = row[i].length();
                }
            }
        }

        // Top border
        printTableBorder(widths, "┌", "┬", "┐");

        // Header row
        System.out.print("│");
        for (int i = 0; i < headers.length; i++) {
            System.out.printf(" " + BOLD + CYAN + "%-" + widths[i] + "s" + RESET + " │", headers[i]);
        }
        System.out.println();

        // Header separator
        printTableBorder(widths, "├", "┼", "┤");

        // Data rows
        if (rows.isEmpty()) {
            System.out.println("│" + YELLOW + "  (no records found)" + RESET);
        } else {
            for (String[] row : rows) {
                System.out.print("│");
                for (int i = 0; i < headers.length; i++) {
                    String cell = (i < row.length && row[i] != null) ? row[i] : "";
                    System.out.printf(" %-" + widths[i] + "s │", cell);
                }
                System.out.println();
            }
        }

        // Bottom border
        printTableBorder(widths, "└", "┴", "┘");
    }

    private static void printTableBorder(int[] widths, String left, String mid, String right) {
        System.out.print(left);
        for (int i = 0; i < widths.length; i++) {
            System.out.print("─".repeat(widths[i] + 2));
            System.out.print(i < widths.length - 1 ? mid : right);
        }
        System.out.println();
    }
}
