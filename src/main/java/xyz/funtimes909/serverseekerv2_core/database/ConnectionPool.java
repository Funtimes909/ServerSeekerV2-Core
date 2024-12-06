package xyz.funtimes909.serverseekerv2_core.database;

import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class ConnectionPool {
    private static final BasicDataSource dataSource = new BasicDataSource();

    public ConnectionPool(String url, String username, String password) {
        dataSource.setUrl("jdbc:postgresql://" + url);
        dataSource.setPassword(password);
        dataSource.setUsername(username);
        dataSource.setMinIdle(30);
    }

    public static Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database!", e);
        }
    }
}
