package perudo.db;

public final class DbConfig {
    private DbConfig() {}
    public static final String DB_URL = "jdbc:postgresql://localhost:5432/test";
    public static final String DB_USER = "postgres";
    public static final String DB_PASSWORD = "password";
}