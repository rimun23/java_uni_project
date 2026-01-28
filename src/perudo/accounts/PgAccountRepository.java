package perudo.accounts;

import perudo.db.Db;

import java.sql.*;
import java.util.*;

public final class PgAccountRepository {

    // -----------------------------
    // Basic CRUD
    // -----------------------------
    public List<Account> findAll() {
        String sql = "SELECT id, username, coins FROM accounts ORDER BY username ASC";
        List<Account> res = new ArrayList<>();
        try (Connection c = Db.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                res.add(new Account(rs.getLong("id"), rs.getString("username"), rs.getInt("coins")));
            }
            return res;
        } catch (SQLException e) {
            throw new RuntimeException("findAll failed: " + e.getMessage(), e);
        }
    }

    public Account findByUsername(String username) {
        if (username == null || username.isBlank()) return null;
        String sql = "SELECT id, username, coins FROM accounts WHERE username = ?";
        try (Connection c = Db.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new Account(rs.getLong("id"), rs.getString("username"), rs.getInt("coins"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByUsername failed: " + e.getMessage(), e);
        }
    }
    Connection connect() throws SQLException{
        return Db.getConnection();
    }
    public Account createIfNotExists(String username) {
        validateUsername(username);

        String insert = "INSERT INTO accounts(username) VALUES (?) ON CONFLICT (username) DO NOTHING";
        try (Connection c = Db.getInstance().getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(insert)) {
                ps.setString(1, username.trim());
                ps.executeUpdate();
            }
            Account acc = findByUsername(username);
            if (acc == null) throw new RuntimeException("Account not found after insert/select.");
            return acc;
        } catch (SQLException e) {
            throw new RuntimeException("createIfNotExists failed: " + e.getMessage(), e);
        }
    }

    // -----------------------------
    // Coins (Win reward)
    // -----------------------------
    public void addCoins(long accountId, int amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be > 0");
        String sql = "UPDATE accounts SET coins = coins + ? WHERE id = ?";
        try (Connection c = Db.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, amount);
            ps.setLong(2, accountId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("addCoins failed: " + e.getMessage(), e);
        }
    }

    // -----------------------------
    // Delete account (cascade)
    // -----------------------------
    public boolean deleteByUsername(String username) {
        validateUsername(username);
        String sql = "DELETE FROM accounts WHERE username = ?";
        try (Connection c = Db.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("deleteByUsername failed: " + e.getMessage(), e);
        }
    }

    // -----------------------------
    // JOIN: Full description (roles + inventory + catalog)
    // -----------------------------
    public FullAccountDescription getFullAccountDescription(long accountId) {
        // One query with JOINs (like GetFullOrderDescription)
        String sql =
                "SELECT a.id AS account_id, a.username, a.coins, " +
                        "       r.role_code, ab.bonus_type, ab.quantity " +
                        "FROM accounts a " +
                        "LEFT JOIN account_roles ar ON ar.account_id = a.id " +
                        "LEFT JOIN roles r ON r.id = ar.role_id " +
                        "LEFT JOIN account_bonuses ab ON ab.account_id = a.id " +
                        "WHERE a.id = ?";

        String username = null;
        Integer coins = null;
        Set<String> roles = new HashSet<>();
        Map<String, Integer> bonuses = new HashMap<>();

        try (Connection c = Db.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    username = rs.getString("username");
                    coins = rs.getInt("coins");

                    String role = rs.getString("role_code");
                    if (role != null) roles.add(role);

                    String bonus = rs.getString("bonus_type");
                    int qty = rs.getInt("quantity");
                    if (bonus != null) bonuses.put(bonus, qty);
                }
                if (!any) throw new RuntimeException("Account not found: id=" + accountId);
            }
        } catch (SQLException e) {
            throw new RuntimeException("getFullAccountDescription failed: " + e.getMessage(), e);
        }

        List<String> roleList = new ArrayList<>(roles);
        roleList.sort(String::compareTo);
        return new FullAccountDescription(accountId, username, coins, roleList, bonuses);
    }

    private static void validateUsername(String username) {
        if (username == null) throw new IllegalArgumentException("username is null");
        String u = username.trim();
        if (u.isEmpty()) throw new IllegalArgumentException("username is empty");
        if (u.length() > 50) throw new IllegalArgumentException("username too long");
        // simple validation: only letters/digits/_/-
        if (!u.matches("[A-Za-z0-9_\\-]+")) {
            throw new IllegalArgumentException("username contains invalid characters");
        }
    }
}
