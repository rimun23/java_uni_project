package perudo.accounts;

import perudo.core.BonusKinds;
import perudo.ui.ConsoleUI;

import java.util.List;

public final class ShopService {
    private ShopService() {}

    // цены — можешь поменять
    public static final int PRICE_REROLL = 30;
    public static final int PRICE_PEEK = 50;

    public static void openShop(ConsoleUI ui, PgAccountRepository accRepo, PgBonusRepository bonusRepo, Account acc) {
        while (true) {
            Account fresh = accRepo.findByUsername(acc.getUsername());
            int coins = fresh.getCoins();

            var inv = bonusRepo.getInventory(fresh.getId());
            int r = inv.getOrDefault(BonusKinds.reroll, 0);
            int p = inv.getOrDefault(BonusKinds.peek, 0);

            ui.println("\n=== SHOP for " + fresh.getUsername() + " ===");
            ui.println("Coins: " + coins);
            ui.println("Inventory: REROLL=" + r + ", PEEK=" + p);
            ui.println("1) Buy REROLL (" + PRICE_REROLL + ")");
            ui.println("2) Buy PEEK   (" + PRICE_PEEK + ")");
            ui.println("3) Exit shop");

            int choice = ui.readInt("Choose: ", 1, 3);
            if (choice == 3) return;

            int qty = ui.readInt("Quantity (1..20): ", 1, 20);

            try {
                if (choice == 1) bonusRepo.buyBonus(fresh.getId(), BonusKinds.reroll, qty, PRICE_REROLL);
                if (choice == 2) bonusRepo.buyBonus(fresh.getId(), BonusKinds.peek, qty, PRICE_PEEK);
                ui.println("Purchased.");
            } catch (RuntimeException ex) {
                ui.println("Purchase failed: " + ex.getMessage());
            }
        }
    }
}

