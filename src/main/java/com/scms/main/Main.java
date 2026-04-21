package com.scms.main;

import com.scms.menu.AdminMenu;
import com.scms.menu.StudentMenu;
import com.scms.util.ConsoleUtil;
import com.scms.util.DBConnection;

import java.sql.Connection;

/**
 * ============================================================
 * Main — Application Entry Point
 * ============================================================
 *
 * Student Course Management System (SCMS)
 *
 * Responsibilities:
 *  1. Test database connectivity on startup
 *  2. Display the main role-selection menu
 *  3. Route to Admin or Student sub-menus
 *  4. Shut down the HikariCP pool on exit
 */
public class Main {

    public static void main(String[] args) {

        printBanner();

        // ── Test DB connection before starting ────────────────────
        if (!testConnection()) {
            System.err.println("\n[ERROR] Cannot connect to database.");
            System.err.println("  → Check PostgreSQL is running.");
            System.err.println("  → Update src/main/resources/db.properties");
            System.err.println("  → Make sure the 'studentdb' database exists.");
            System.exit(1);
        }

        ConsoleUtil.printSuccess("Database connection successful!");

        // ── Main application loop ─────────────────────────────────
        boolean running = true;
        while (running) {
            ConsoleUtil.printHeader("SCMS — Main Menu");
            System.out.println("  Who are you?");
            System.out.println();
            System.out.println("  [1]  " + ConsoleUtil.CYAN + "Admin" + ConsoleUtil.RESET
                    + "   (manage students, courses, grades)");
            System.out.println("  [2]  " + ConsoleUtil.GREEN + "Student" + ConsoleUtil.RESET
                    + " (register, enroll, view grades)");
            System.out.println("  [0]  Exit");
            ConsoleUtil.printDivider();

            String choice = ConsoleUtil.prompt("Select role");

            switch (choice) {
                case "1" -> new AdminMenu().show();
                case "2" -> new StudentMenu().show();
                case "0" -> {
                    running = false;
                    shutdown();
                }
                default -> ConsoleUtil.printError("Invalid choice. Enter 1, 2, or 0.");
            }
        }
    }

    // ─── Test that DB is reachable ────────────────────────────────

    private static boolean testConnection() {
        System.out.println("\n[Startup] Testing database connection...");
        try (Connection conn = DBConnection.getPooledConnection()) {
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            System.err.println("[Startup] Connection error: " + e.getMessage());
            return false;
        }
    }

    // ─── Graceful shutdown ────────────────────────────────────────

    private static void shutdown() {
        System.out.println();
        ConsoleUtil.printInfo("Shutting down...");
        DBConnection.closePool();
        System.out.println(ConsoleUtil.GREEN
                + "  Thank you for using SCMS. Goodbye!"
                + ConsoleUtil.RESET);
        System.out.println();
    }

    // ─── ASCII Banner ─────────────────────────────────────────────

    private static void printBanner() {
        System.out.println(ConsoleUtil.CYAN);
        System.out.println("  ███████╗ ██████╗███╗   ███╗███████╗");
        System.out.println("  ██╔════╝██╔════╝████╗ ████║██╔════╝");
        System.out.println("  ███████╗██║     ██╔████╔██║███████╗");
        System.out.println("  ╚════██║██║     ██║╚██╔╝██║╚════██║");
        System.out.println("  ███████║╚██████╗██║ ╚═╝ ██║███████║");
        System.out.println("  ╚══════╝ ╚═════╝╚═╝     ╚═╝╚══════╝");
        System.out.println(ConsoleUtil.RESET);
        System.out.println("  Student Course Management System v1.0");
        System.out.println("  Built with Java 17 + JDBC + PostgreSQL + HikariCP");
        System.out.println();
    }
}
