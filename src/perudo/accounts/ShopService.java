package perudo.accounts;

import perudo.core.BonusKinds;
import perudo.db.DbConfig;
import perudo.security.PgRoleRepository;
import perudo.security.RoleKinds;
import perudo.ui.ConsoleUI;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public final class ShopService {
    private ShopService() {}

    private static final int FALLBACK_PRICE_REROLL = 30;
    private static final int FALLBACK_PRICE_PEEK   = 50;

    public static void openShop(ConsoleUI ui,
                                PgAccountRepository accRepo,
                                PgBonusRepository bonusRepo,
                                Account account) {

        long accountId = account.getId();
        PgRoleRepository roleRepo = new PgRoleRepository();

        while (true) {
            // refresh account (coins can change)
            Account fresh = safeReloadAccount(accRepo, account.getUsername());
            if (fresh != null) account = fresh;

            int coins = account.getCoins();
            Map<String, Integer> inv = safeInventory(bonusRepo, accountId);

            // ✅ роли подгружаем КАЖДЫЙ раз (чтобы всегда актуально)
            Set<String> roles = roleRepo.getRolesForAccount(accountId);
            boolean isAdmin = roles.contains(RoleKinds.ADMIN);
            boolean isManager = roles.contains(RoleKinds.MANAGER) || isAdmin; // admin тоже менеджер по правам

            ui.println("\n=== SHOP ===");
            ui.println("Account: " + account.getUsername());
            ui.println("Coins: " + coins);
            ui.println("Roles: " + roles);
            ui.println("Inventory: REROLL=" + inv.getOrDefault(BonusKinds.reroll, 0)
                    + ", PEEK=" + inv.getOrDefault(BonusKinds.peek, 0));

            // Load products from DB catalog (JOIN categories)
            List<BonusProduct> products = loadCatalogProducts();
            if (products.isEmpty()) {
                products = List.of(
                        new BonusProduct("GAMEPLAY", BonusKinds.reroll, "Reroll your dice (1x per match)", FALLBACK_PRICE_REROLL),
                        new BonusProduct("GAMEPLAY", BonusKinds.peek, "Peek one bot dice (1x per match)", FALLBACK_PRICE_PEEK)
                );
            }

            // Group by category (lambda/streams requirement)
            Map<String, List<BonusProduct>> byCategory = products.stream()
                    .sorted(Comparator.comparing(BonusProduct::category).thenComparing(BonusProduct::displayName))
                    .collect(Collectors.groupingBy(BonusProduct::category, LinkedHashMap::new, Collectors.toList()));

            // Build menu: number -> handler
            Map<Integer, Runnable> handlers = new HashMap<>();
            List<BonusProduct> flatMenu = new ArrayList<>();

            ui.println("\nProducts:");
            int idx = 1;

            for (Map.Entry<String, List<BonusProduct>> e : byCategory.entrySet()) {
                ui.println("[" + e.getKey() + "]");
                for (BonusProduct p : e.getValue()) {
                    final int num = idx;
                    ui.println("  " + num + ") " + p.displayName() + " | code=" + p.code() + " | price=" + p.price());
                    flatMenu.add(p);

                    handlers.put(num, () -> {
                        int qty = ui.readInt("Quantity (1..20): ", 1, 20);
                        try {
                            bonusRepo.buyBonus(accountId, p.code(), qty, p.price());
                            ui.println("Purchased: " + qty + " x " + p.code());
                        } catch (RuntimeException ex) {
                            ui.println("Purchase failed: " + ex.getMessage());
                        }
                    });

                    idx++;
                }
            }

            // ✅ secured endpoints
            if (isManager) {
                final int num = idx;
                ui.println("\n" + num + ") [MANAGER] Grant coins to an account");
                handlers.put(num, () -> handleGrantCoins(ui, accRepo));
                idx++;
            }

            if (isAdmin) {
                final int num = idx;
                ui.println(idx + ") [ADMIN] Delete an account");
                Account finalAccount = account;
                handlers.put(num, () -> handleDeleteAccount(ui, accRepo, finalAccount.getUsername()));
                idx++;
            }

            ui.println("\n" + idx + ") Exit shop");
            int exitNum = idx;

            int choice = ui.readInt("Choose: ", 1, exitNum);
            if (choice == exitNum) return;

            Runnable handler = handlers.get(choice);
            if (handler == null) {
                ui.println("Unknown option.");
                continue;
            }
            handler.run();
        }
    }

    // -----------------------------
    // Secured actions
    // -----------------------------
    private static void handleGrantCoins(ConsoleUI ui, PgAccountRepository accRepo) {
        ui.println("\n=== GRANT COINS (MANAGER/ADMIN) ===");
        String targetUsername = ui.readNonEmpty("Target username: ");
        Account target = accRepo.findByUsername(targetUsername);
        if (target == null) {
            ui.println("Account not found.");
            return;
        }
        int amount = ui.readInt("Coins to add (1..100000): ", 1, 100000);
        accRepo.addCoins(target.getId(), amount);
        ui.println("Granted +" + amount + " coins to " + target.getUsername());
    }

    private static void handleDeleteAccount(ConsoleUI ui, PgAccountRepository accRepo, String currentUsername) {
        ui.println("\n=== DELETE ACCOUNT (ADMIN) ===");
        String targetUsername = ui.readNonEmpty("Username to delete: ");

        if (targetUsername.equalsIgnoreCase(currentUsername)) {
            ui.println("You cannot delete the currently selected account.");
            return;
        }

        Account target = accRepo.findByUsername(targetUsername);
        if (target == null) {
            ui.println("Account not found.");
            return;
        }

        ui.println("Type DELETE to confirm deletion of '" + target.getUsername() + "'");
        String confirm = ui.readLine().trim();
        if (!"DELETE".equals(confirm)) {
            ui.println("Cancelled.");
            return;
        }

        boolean ok = accRepo.deleteByUsername(target.getUsername());
        ui.println(ok ? "Deleted." : "Delete failed.");
    }

    // -----------------------------
    // Helpers
    // -----------------------------
    private static Account safeReloadAccount(PgAccountRepository accRepo, String username) {
        try {
            return accRepo.findByUsername(username);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static Map<String, Integer> safeInventory(PgBonusRepository bonusRepo, long accountId) {
        try {
            return bonusRepo.getInventory(accountId);
        } catch (RuntimeException ex) {
            return new HashMap<>();
        }
    }

    /**
     * JOIN example:
     * bonus_catalog + bonus_categories => category_name, bonus_code, display_name, price
     */
    private static List<BonusProduct> loadCatalogProducts() {
        String sql =
                "SELECT c.category_name, p.bonus_code, p.display_name, p.price " +
                        "FROM bonus_catalog p " +
                        "JOIN bonus_categories c ON c.id = p.category_id " +
                        "ORDER BY c.category_name, p.display_name";

        List<BonusProduct> res = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(DbConfig.DB_URL, DbConfig.DB_USER, DbConfig.DB_PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String category = rs.getString("category_name");
                String code = rs.getString("bonus_code");
                String name = rs.getString("display_name");
                int price = rs.getInt("price");

                if (code == null || code.isBlank()) continue;
                if (price <= 0) continue;

                res.add(new BonusProduct(category, code, name, price));
            }

        } catch (SQLException e) {
            return Collections.emptyList();
        }

        return res;
    }

    // -----------------------------
    // Small immutable model
    // -----------------------------
    private static final class BonusProduct {
        private final String category;
        private final String code;
        private final String displayName;
        private final int price;

        private BonusProduct(String category, String code, String displayName, int price) {
            this.category = (category == null || category.isBlank()) ? "UNCATEGORIZED" : category;
            this.code = code;
            this.displayName = (displayName == null || displayName.isBlank()) ? code : displayName;
            this.price = price;
        }

        public String category() { return category; }
        public String code() { return code; }
        public String displayName() { return displayName; }
        public int price() { return price; }
    }
}
