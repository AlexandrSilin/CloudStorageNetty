package server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Connect {
    private static final String DB_URL = "jdbc:mysql://127.0.0.1:3306/cloud";
    private static final String USER = "root";
    private static final String PASS = "123456";
    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    private static Connection instance;

    private Connect() {

    }

    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName(DRIVER);
        return instance == null ? instance = DriverManager.getConnection(DB_URL, USER, PASS) : instance;
    }
}
