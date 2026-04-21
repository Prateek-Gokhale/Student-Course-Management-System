package com.scms.service;

import com.scms.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * ============================================================
 * PerformanceDemo — Connection Pool vs Non-Pool Benchmark
 * ============================================================
 *
 * Demonstrates the performance benefit of HikariCP connection pooling
 * by comparing the time to run the same query N times using:
 *
 *   1. Fresh DriverManager connections each time (no pool)
 *   2. HikariCP pooled connections (reused from pool)
 *
 * Opening a new TCP connection to PostgreSQL every time is expensive.
 * With a pool, the connection is already open and ready to use.
 */
public class PerformanceDemo {

    private static final int ITERATIONS = 50; // Number of test queries

    public void runComparison() {
        System.out.println("\n╔══════════════════════════════════════════════════╗");
        System.out.println("║       Connection Pool Performance Comparison     ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
        System.out.println("  Running " + ITERATIONS + " queries each way...\n");

        long withoutPool = testWithoutPool();
        long withPool    = testWithPool();

        System.out.println("\n──────────────────────────────────────────────────");
        System.out.printf("  %-30s %6d ms%n", "Without pooling (DriverManager):", withoutPool);
        System.out.printf("  %-30s %6d ms%n", "With HikariCP pooling:",            withPool);
        System.out.printf("  %-30s %.1fx faster%n", "Pool speedup:",
                (double) withoutPool / Math.max(withPool, 1));
        System.out.println("──────────────────────────────────────────────────");
        System.out.println("  Pool eliminates the overhead of opening a new");
        System.out.println("  TCP/TLS connection to PostgreSQL on every request.");
        System.out.println("──────────────────────────────────────────────────\n");
    }

    // ─── Test 1: New connection every time ────────────────────────

    private long testWithoutPool() {
        long start = System.currentTimeMillis();

        for (int i = 0; i < ITERATIONS; i++) {
            // Each iteration opens and closes a brand-new connection
            try (Connection conn = DBConnection.getConnectionMethod1();
                 PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM students")) {

                ps.executeQuery(); // Execute the query

            } catch (Exception e) {
                System.err.println("[Without Pool] Error: " + e.getMessage());
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        System.out.println("  [Without Pool] " + ITERATIONS + " queries: " + elapsed + " ms");
        return elapsed;
    }

    // ─── Test 2: Pooled connection (borrowed and returned) ────────

    private long testWithPool() {
        long start = System.currentTimeMillis();

        for (int i = 0; i < ITERATIONS; i++) {
            // getPooledConnection() borrows from pool (already open TCP connection)
            // .close() returns it to pool — no actual TCP close
            try (Connection conn = DBConnection.getPooledConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM students")) {

                ps.executeQuery();

            } catch (Exception e) {
                System.err.println("[With Pool] Error: " + e.getMessage());
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        System.out.println("  [With Pool]    " + ITERATIONS + " queries: " + elapsed + " ms");
        return elapsed;
    }
}
