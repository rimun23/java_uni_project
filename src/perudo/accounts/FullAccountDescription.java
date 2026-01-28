package perudo.accounts;

import java.util.List;
import java.util.Map;

public final class FullAccountDescription {
    private final long accountId;
    private final String username;
    private final int coins;
    private final List<String> roles;
    private final Map<String, Integer> bonuses; // bonus_code -> quantity

    public FullAccountDescription(long accountId, String username, int coins,
                                  List<String> roles, Map<String, Integer> bonuses) {
        this.accountId = accountId;
        this.username = username;
        this.coins = coins;
        this.roles = roles;
        this.bonuses = bonuses;
    }

    public long getAccountId() { return accountId; }
    public String getUsername() { return username; }
    public int getCoins() { return coins; }
    public List<String> getRoles() { return roles; }
    public Map<String, Integer> getBonuses() { return bonuses; }
}

