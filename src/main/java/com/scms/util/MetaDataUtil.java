package com.scms.util;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * MetaDataUtil - JDBC Metadata Demonstration
 * ============================================================
 *
 * Demonstrates two important JDBC metadata APIs:
 *
 *  1. DatabaseMetaData  — information about the DATABASE itself
 *     (version, driver, tables, supported features, etc.)
 *
 *  2. ResultSetMetaData — information about the COLUMNS in a query result
 *     (column names, types, sizes — dynamic column printing)
 */
public class MetaDataUtil {

    /**
     * Prints information about the connected database.
     * Uses DatabaseMetaData — available from any Connection object.
     */
    public static void printDatabaseInfo(Connection connection) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();

        ConsoleUtil.printHeader("Database Metadata");

        System.out.printf("  %-30s %s%n", "Database Product:",    meta.getDatabaseProductName());
        System.out.printf("  %-30s %s%n", "Database Version:",    meta.getDatabaseProductVersion());
        System.out.printf("  %-30s %s%n", "JDBC Driver:",         meta.getDriverName());
        System.out.printf("  %-30s %s%n", "Driver Version:",      meta.getDriverVersion());
        System.out.printf("  %-30s %s%n", "JDBC Version:",
                meta.getJDBCMajorVersion() + "." + meta.getJDBCMinorVersion());
        System.out.printf("  %-30s %s%n", "URL:",                 meta.getURL());
        System.out.printf("  %-30s %s%n", "User:",                meta.getUserName());
        System.out.printf("  %-30s %s%n", "Max Connections:",     meta.getMaxConnections());
        System.out.printf("  %-30s %s%n", "Supports Transactions:", meta.supportsTransactions());
        System.out.printf("  %-30s %s%n", "Supports Savepoints:",   meta.supportsSavepoints());
        System.out.printf("  %-30s %s%n", "Supports Batch Updates:", meta.supportsBatchUpdates());
        System.out.printf("  %-30s %s%n", "Read Only:",             connection.isReadOnly());
    }

    /**
     * Lists all tables in the 'public' schema of the connected database.
     * Uses DatabaseMetaData.getTables()
     */
    public static void listTables(Connection connection) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();

        ConsoleUtil.printHeader("Tables in Database");

        // getTables(catalog, schemaPattern, tableNamePattern, types[])
        ResultSet rs = meta.getTables(null, "public", "%", new String[]{"TABLE"});

        List<String[]> rows = new ArrayList<>();
        while (rs.next()) {
            rows.add(new String[]{
                rs.getString("TABLE_NAME"),
                rs.getString("TABLE_TYPE"),
                rs.getString("TABLE_SCHEM")
            });
        }
        rs.close();

        ConsoleUtil.printTable(new String[]{"Table Name", "Type", "Schema"}, rows);
    }

    /**
     * Prints column metadata for a specific table.
     * Shows: column name, type, size, nullable
     */
    public static void printColumnInfo(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();

        ConsoleUtil.printHeader("Column Info: " + tableName);

        ResultSet rs = meta.getColumns(null, "public", tableName, "%");

        List<String[]> rows = new ArrayList<>();
        while (rs.next()) {
            rows.add(new String[]{
                rs.getString("COLUMN_NAME"),
                rs.getString("TYPE_NAME"),
                rs.getString("COLUMN_SIZE"),
                rs.getString("IS_NULLABLE"),
                rs.getString("COLUMN_DEF") != null ? rs.getString("COLUMN_DEF") : "—"
            });
        }
        rs.close();

        ConsoleUtil.printTable(
            new String[]{"Column", "Type", "Size", "Nullable", "Default"},
            rows
        );
    }

    /**
     * ============================================================
     * Dynamic Column Printer using ResultSetMetaData
     * ============================================================
     *
     * Instead of hardcoding column names, we inspect the ResultSet
     * at runtime to find column names and types.
     *
     * This is useful for building generic query tools.
     *
     * @param rs A ResultSet from any SQL query
     */
    public static void printResultSetDynamic(ResultSet rs) throws SQLException {
        ResultSetMetaData rsMeta = rs.getMetaData();
        int columnCount = rsMeta.getColumnCount();

        // ── Build headers dynamically ──────────────────────────────
        String[] headers = new String[columnCount];
        for (int i = 1; i <= columnCount; i++) {
            // Combine column name + type for display (e.g., "name (varchar)")
            headers[i - 1] = rsMeta.getColumnName(i)
                    + " (" + rsMeta.getColumnTypeName(i) + ")";
        }

        // ── Collect rows ──────────────────────────────────────────
        List<String[]> rows = new ArrayList<>();
        while (rs.next()) {
            String[] row = new String[columnCount];
            for (int i = 1; i <= columnCount; i++) {
                Object val = rs.getObject(i);
                row[i - 1] = (val != null) ? val.toString() : "NULL";
            }
            rows.add(row);
        }

        ConsoleUtil.printTable(headers, rows);
    }
}
