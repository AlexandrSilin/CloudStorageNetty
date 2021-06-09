package server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Connect {
    private static Connection instance;

    private Connect() {

    }

    /**
     * Подключение к БД
     * @return Connection to DB
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return instance == null ? instance = DriverManager
                .getConnection("jdbc:mysql://127.0.0.1:3306/cloud", "root", "123456") : instance;
    }
}
