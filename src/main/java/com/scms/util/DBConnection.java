package com.scms.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * ============================================================
 * DBConnection - Central Database Utility Class
 * ============================================================
 *
 * Demonstrates 3 JDBC connection methods:
 *   1. URL + Username + Password (most common)
 *   2. Single JDBC URL with credentials embedded
 *   3. Properties object
 *
 * Also provides:
 *   - HikariCP connection pool (recommended for production)
 *   - A simple non-pooled fallback connection
 *
 * Usage:
 *   Connection con = DBConnection.getPooledConnection();
 *   // ... use connection ...
 *   con.close();  // returns it to pool, does NOT actually close
 */
public class DBConnection {

    // ─── Loaded from db.properties ────────────────────────────────
    private static String DB_URL;
    private static String DB_USER;
    private static String DB_PASSWORD;

    // ─── HikariCP DataSource (singleton) ──────────────────────────
    private static HikariDataSource hikariDataSource;

    // Static block: runs once when the class is first loaded
    static {
        loadProperties();
        initHikariPool();
    }

    // ─── Load properties from db.properties file ──────────────────

    private static void loadProperties() {
        Properties props = new Properties();
        try (InputStream input = DBConnection.class
                .getClassLoader()
                .getResourceAsStream("db.properties")) {

            if (input == null) {
                throw new RuntimeException("db.properties not found in classpath!");
            }

            props.load(input);
            DB_URL      = props.getProperty("db.url");
            DB_USER     = props.getProperty("db.username");
            DB_PASSWORD = props.getProperty("db.password");

        } catch (IOException e) {
            throw new RuntimeException("Failed to load db.properties", e);
        }
    }

    // ─── Initialize HikariCP Pool ──────────────────────────────────

    private static void initHikariPool() {
        HikariConfig config = new HikariConfig();

        // Load HikariCP settings from properties file
        Properties props = new Properties();
        try (InputStream input = DBConnection.class
                .getClassLoader()
                .getResourceAsStream("db.properties")) {
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load pool config", e);
        }

        config.setJdbcUrl(DB_URL);
        config.setUsername(DB_USER);
        config.setPassword(DB_PASSWORD);

        // Pool sizing
        config.setMaximumPoolSize(Integer.parseInt(props.getProperty("pool.maximumPoolSize", "10")));
        config.setMinimumIdle(Integer.parseInt(props.getProperty("pool.minimumIdle", "2")));

        // Timeouts (in ms)
        config.setConnectionTimeout(Long.parseLong(props.getProperty("pool.connectionTimeout", "30000")));
        config.setIdleTimeout(Long.parseLong(props.getProperty("pool.idleTimeout", "600000")));
        config.setMaxLifetime(Long.parseLong(props.getProperty("pool.maxLifetime", "1800000")));

        config.setPoolName(props.getProperty("pool.poolName", "SCMS-Pool"));

        // Optional: test query to validate connections
        config.setConnectionTestQuery("SELECT 1");

        hikariDataSource = new HikariDataSource(config);
        System.out.println("[Pool] HikariCP pool initialized: " + config.getPoolName());
    }

    // ─────────────────────────────────────────────────────────────
    // CONNECTION METHOD 1: URL + Username + Password
    // The most common and readable approach.
    // ─────────────────────────────────────────────────────────────

    public static Connection getConnectionMethod1() throws SQLException {
        System.out.println("[JDBC Method 1] Connecting via URL + username + password");
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    // ─────────────────────────────────────────────────────────────
    // CONNECTION METHOD 2: Single JDBC URL with embedded credentials
    // Embeds user/password directly in the URL query string.
    // Useful for quick testing, NOT recommended for production.
    // ─────────────────────────────────────────────────────────────

    public static Connection getConnectionMethod2() throws SQLException {
        // Appends ?user=...&password=... to the URL
        String urlWithCreds = DB_URL + "?user=" + DB_USER + "&password=" + DB_PASSWORD;
        System.out.println("[JDBC Method 2] Connecting via single embedded URL");
        return DriverManager.getConnection(urlWithCreds);
    }

    // ─────────────────────────────────────────────────────────────
    // CONNECTION METHOD 3: Properties Object
    // Allows passing extra driver-specific settings beyond user/password.
    // Useful for SSL, application name, socket timeout, etc.
    // ─────────────────────────────────────────────────────────────

    public static Connection getConnectionMethod3() throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", DB_USER);
        props.setProperty("password", DB_PASSWORD);
        props.setProperty("ApplicationName", "SCMS");           // PostgreSQL-specific
        props.setProperty("connectTimeout", "10");              // seconds
        props.setProperty("socketTimeout", "30");               // seconds

        System.out.println("[JDBC Method 3] Connecting via Properties object");
        return DriverManager.getConnection(DB_URL, props);
    }

    // ─────────────────────────────────────────────────────────────
    // POOLED CONNECTION (RECOMMENDED FOR ALL APP CODE)
    // Borrows a connection from HikariCP pool.
    // Calling .close() returns it to the pool — it's NOT destroyed.
    // ─────────────────────────────────────────────────────────────

    public static Connection getPooledConnection() throws SQLException {
        return hikariDataSource.getConnection();
    }

    // ─── Graceful shutdown ────────────────────────────────────────

    public static void closePool() {
        if (hikariDataSource != null && !hikariDataSource.isClosed()) {
            hikariDataSource.close();
            System.out.println("[Pool] HikariCP pool closed.");
        }
    }

    // ─── Helper: quietly close a connection ───────────────────────

    public static void closeQuietly(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("[Warning] Failed to close connection: " + e.getMessage());
            }
        }
    }
}
