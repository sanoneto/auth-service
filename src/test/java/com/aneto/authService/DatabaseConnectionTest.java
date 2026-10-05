package com.aneto.authService;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.DriverManager;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DatabaseConnectionTest {

    @Test
    void testDirectJdbcConnection() throws Exception {
        String url = "jdbc:postgresql://127.0.0.1:5432/horas_db?sslmode=disable";
        try (Connection conn = DriverManager.getConnection(url, "horas_user", "horas_pass")) {
            assertNotNull(conn);
            assertFalse(conn.isClosed());
            System.out.println(">>> JDBC Connection Successful: " + conn.getMetaData().getDatabaseProductName() + " " + conn.getMetaData().getDatabaseProductVersion());
        }
    }
}
