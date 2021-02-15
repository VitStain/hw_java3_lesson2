package chat.auth;

import java.sql.*;

public class BaseAuthService implements AuthService {

    private static Connection connection;
    private static Statement stmt;
    private static ResultSet rs;

    private  static void connection() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:C:\\Users\\Виталий\\Desktop\\java_ult\\ChatServer\\src\\main\\resources\\db\\main.db");
        stmt = connection.createStatement();
    }

    private  static void disconnection() throws SQLException {
        connection.close();
    }


    @Override
    public void start() {
        System.out.println("Сервис аутентификации запущен");
    }

    @Override
    public String getUsernameByLoginAndPassword(String login, String password) throws SQLException, ClassNotFoundException {
        connection();
        rs = stmt.executeQuery(String.format("SELECT password, username FROM users WHERE login = '%s'", login));
        String username = rs.getString("username");
        System.out.println(rs.getString("username"));
        if(rs.getString("password").equals(password)) {
            disconnection();
            return username;
            }
        disconnection();
        return null;
    }

    @Override
    public void close() {
        System.out.println("Сервис аутентификации завершен");

    }
}
