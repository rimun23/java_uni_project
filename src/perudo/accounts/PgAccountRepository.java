package perudo.accounts;
import java.sql.*;
import java.util.*;
public class PgAccountRepository {
    private final String url;
    private final String user;
    private final String pass;
    public PgAccountRepository(String url, String user, String pass){
        this.url = url;
        this.user = user;
        this.pass = pass;
    }
    Connection connect() throws SQLException{
        return DriverManager.getConnection(url, user, pass);
    }
    public List<Account> findAll(){
        String sql = "SELECT id, username, coins FROM accounts ORDER BY username ASC";
        List<Account> res = new ArrayList<>();
        try(Connection connect = connect();
            PreparedStatement prepSt = connect.prepareStatement(sql);
            ResultSet resSet = prepSt.executeQuery()){
            while(resSet.next()){
                res.add(new Account(resSet.getLong("id"), resSet.getString("username"), resSet.getInt("coins")));
            }
            return res;
        } catch (SQLException e){
            throw new RuntimeException("DataBase findALL failed");
        }
    }
    public Account findByUsername(String username) {
        String sql = "SELECT id, username, coins FROM accounts WHERE username = ?";
        try (Connection c = connect();
             PreparedStatement prepSt = c.prepareStatement(sql)) {
            prepSt.setString(1, username);
            try (ResultSet resSet = prepSt.executeQuery()) {
                if (resSet.next()) {
                    return new Account(resSet.getLong("id"), resSet.getString("username"), resSet.getInt("coins"));
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("DataBase findByUsername failed: " + e.getMessage(), e);
        }
    }
    public Account createIfNotExists(String username){
        String insert = "INSERT INTO accounts(username) VALUES (?) ON CONFLICT (username) DO NOTHING";
        try(Connection connect = connect()){
            try(PreparedStatement prepSt = connect.prepareStatement(insert)){
                prepSt.setString(1, username);
                prepSt.executeUpdate();
            }
            Account acc = findByUsername(username);
            if(acc == null) throw new RuntimeException("Account not found after insert or select");
            return acc;
        }catch (SQLException e){
            throw new RuntimeException("DataBase createIfNotExists failed: " + e.getMessage(), e);
        }
    }

}
