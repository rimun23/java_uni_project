package perudo.db;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
public final class Db {
    private static final Db INSTANCE = new Db();
    private Db() {}
    public static Db getInstance() {
        return INSTANCE;
    }
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                DbConfig.DB_URL,
                DbConfig.DB_USER,
                DbConfig.DB_PASSWORD
        );
    }
}