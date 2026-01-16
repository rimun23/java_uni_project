package perudo.accounts;
import java.sql.*;
import java.util.*;
public class PgBonusRepository {
    private final PgAccountRepository accountRepo;
    public PgBonusRepository(PgAccountRepository accountRepo){
        this.accountRepo = accountRepo;
    }
    public Map<String, Integer> getInventory(long accountId) {
        String sql = "SELECT bonus_type, quantity FROM account_bonuses WHERE account_id = ?";
        Map<String, Integer> inv = new HashMap<>();
        try (Connection connect = accountRepo.connect();
             PreparedStatement prepSt = connect.prepareStatement(sql)) {
            prepSt.setLong(1, accountId);
            try (ResultSet rs = prepSt.executeQuery()) {
                while (rs.next()) {
                    inv.put(rs.getString("bonus_type"), rs.getInt("quantity"));
                }
            }
            return inv;
        } catch (SQLException e) {
            throw new RuntimeException("DB getInventory failed: " + e.getMessage(), e);
        }
    }
    public void buyBonus(long accountId, String bonusType, int qty, int pricePerItem) {
        if (qty <= 0) throw new IllegalArgumentException("qty must be > 0");
        int totalCost = qty * pricePerItem;

        String lockAcc = "SELECT coins FROM accounts WHERE id = ? FOR UPDATE";
        String updateCoins = "UPDATE accounts SET coins = coins - ? WHERE id = ?";
        String upsertBonus =
                "INSERT INTO account_bonuses(account_id, bonus_type, quantity) " +
                        "VALUES (?, ?, ?) " +
                        "ON CONFLICT (account_id, bonus_type) DO UPDATE SET quantity = account_bonuses.quantity + EXCLUDED.quantity";

        try (Connection c = accountRepo.connect()) {
            c.setAutoCommit(false);

            int coins;
            try (PreparedStatement ps = c.prepareStatement(lockAcc)) {
                ps.setLong(1, accountId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new RuntimeException("Account not found");
                    coins = rs.getInt("coins");
                }
            }

            if (coins < totalCost) {
                c.rollback();
                throw new RuntimeException("Not enough coins. Need " + totalCost + ", have " + coins);
            }

            try (PreparedStatement ps = c.prepareStatement(updateCoins)) {
                ps.setInt(1, totalCost);
                ps.setLong(2, accountId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = c.prepareStatement(upsertBonus)) {
                ps.setLong(1, accountId);
                ps.setString(2, bonusType);
                ps.setInt(3, qty);
                ps.executeUpdate();
            }

            c.commit();
        } catch (SQLException e) {
            throw new RuntimeException("DB buyBonus failed: " + e.getMessage(), e);
        }
    }

    /** Decrease inventory by 1 if possible. Returns true if consumed. */
    public boolean consumeOne(long accountId, String bonusType) {
        String sql =
                "UPDATE account_bonuses " + "SET quantity = quantity - 1 " + "WHERE account_id = ? AND bonus_type = ? AND quantity > 0";
        try (Connection c = accountRepo.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            ps.setString(2, bonusType);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("DB consumeOne failed: " + e.getMessage(), e);
        }
    }
}

