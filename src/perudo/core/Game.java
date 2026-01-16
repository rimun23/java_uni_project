package perudo.core;

import perudo.accounts.*;
import perudo.ai.*;
import perudo.players.*;
import perudo.ui.ConsoleUI;

import java.util.*;

/**
 * Game.java (FULL)
 * - Account selection from PostgreSQL
 * - Shop before game (buy bonuses with coins)
 * - Bonuses are inventory-based (DB) and also limited to 1 use per match per player (PlayerWallet)
 *   * REROLL: reroll your dice (consumes 1 from DB)
 *   * PEEK: view one bot's dice (consumes 1 from DB)
 */
public final class Game {
    private final List<Player> players;
    private final ConsoleUI ui;
    private final RuleEngine rules;
    private final PgBonusRepository bonusRepo;
    private int currentIndex;

    private Game(List<Player> players, ConsoleUI ui, RuleEngine rules, PgBonusRepository bonusRepo, int startIndex) {
        this.players = players;
        this.ui = ui;
        this.rules = rules;
        this.bonusRepo = bonusRepo;
        this.currentIndex = startIndex;
    }

    public void play() {
        ui.println("=== PERUDO (Liar's Dice) ===");

        while (alivePlayers() > 1) {
            playRound();
        }

        Player winner = players.stream().filter(Player::isAlive).findFirst().orElse(null);
        ui.println("\nWinner: " + (winner != null ? winner.name() : "nobody"));
    }

    private void playRound() {
        ui.println("\n--- New Round ---");
        RoundContext ctx = new RoundContext(players);

        // Roll for alive players
        for (Player p : players) {
            if (p.isAlive()) p.roll();
        }

        // Show only humans their dice
        for (Player p : players) {
            if (p.isAlive() && p instanceof HumanPlayer) {
                ui.println(p.name() + " dice: " + p.cup().sortedString());
            }
        }

        int turn = currentIndex;

        while (true) {
            turn = nextAliveIndex(turn);
            Player p = players.get(turn);

            ui.println("\nTurn: " + p.name() + " (dice: " + p.diceCount() + ")");
            ui.println("Current bid: " + (ctx.currentBid() == null ? "none" : ctx.currentBid()));

            Action action = p.chooseAction(ctx, ui);

            // ===== BONUSES (do NOT end round; player continues choosing) =====
            if (action.kind() == ActionKinds.BONUS_REROLL) {
                if (!(p instanceof HumanPlayer)) {
                    ui.println("Only humans can use bonuses.");
                    continue;
                }
                HumanPlayer hp = (HumanPlayer) p;
                PlayerWallet w = hp.wallet();

                if (!w.canUseReroll()) {
                    ui.println("REROLL not available (0 in inventory or already used this match).");
                    continue;
                }

                boolean consumed = bonusRepo.consumeOne(w.getAccountId(), BonusKinds.reroll);
                if (!consumed) {
                    ui.println("REROLL not available in DB (inventory desync).");
                    continue;
                }

                w.decrementRerollLocal();
                w.markRerollUsed();

                p.roll(); // reroll only this player's dice
                ui.println(p.name() + " used REROLL. New dice: " + p.cup().sortedString());
                continue;
            }

            if (action.kind() == ActionKinds.BONUS_PEEK) {
                if (!(p instanceof HumanPlayer)) {
                    ui.println("Only humans can use bonuses.");
                    continue;
                }
                HumanPlayer hp = (HumanPlayer) p;
                PlayerWallet w = hp.wallet();

                if (!ctx.hasAliveBots()) {
                    ui.println("No alive bots to peek.");
                    continue;
                }
                if (!w.canUsePeek()) {
                    ui.println("PEEK not available (0 in inventory or already used this match).");
                    continue;
                }

                int target = action.target();
                if (target < 0 || target >= ctx.players().size() || !ctx.players().get(target).isAlive()) {
                    ui.println("Invalid peek target.");
                    continue;
                }
                if (!(ctx.players().get(target) instanceof BotPlayer)) {
                    ui.println("You can peek only bots.");
                    continue;
                }

                boolean consumed = bonusRepo.consumeOne(w.getAccountId(), BonusKinds.peek);
                if (!consumed) {
                    ui.println("PEEK not available in DB (inventory desync).");
                    continue;
                }

                w.decrementPeekLocal();
                w.markPeekUsed();

                ui.println(p.name() + " used PEEK.");
                ui.println("Peek " + ctx.players().get(target).name() + " dice: " +
                        ctx.players().get(target).cup().sortedString());
                continue;
            }

            // ===== NORMAL ACTIONS =====
            if (action.kind() == ActionKinds.BID) {
                Bid bid = action.bid();

                if (ctx.currentBid() != null && !bid.isHigherThan(ctx.currentBid())) {
                    if (p instanceof HumanPlayer) {
                        ui.println("Invalid bid. Must be higher than current bid.");
                        continue; // retry
                    } else {
                        bid = ctx.currentBid().nextMinimumBid();
                    }
                }

                ctx.setBid(bid, turn);
                ui.println(p.name() + " bids: " + bid);
                continue;
            }

            // Can't call before first bid
            if (ctx.currentBid() == null) {
                ui.println("You can't call before the first bid.");
                continue;
            }

            if (action.kind() == ActionKinds.LIAR) {
                rules.resolveLiar(ctx, turn, ui);
                currentIndex = ctx.nextStarterIndex();
                break;
            }

            if (action.kind() == ActionKinds.EXACT) {
                rules.resolveExact(ctx, turn, ui);
                currentIndex = ctx.nextStarterIndex();
                break;
            }

            ui.println("Unknown action.");
        }
    }

    private int alivePlayers() {
        int c = 0;
        for (Player p : players) if (p.isAlive()) c++;
        return c;
    }

    private int nextAliveIndex(int fromIndex) {
        int n = players.size();
        int i = fromIndex;
        for (int k = 0; k < n; k++) {
            i = (i + 1) % n;
            if (players.get(i).isAlive()) return i;
        }
        return fromIndex;
    }

    // =====================================================================
    //                         FACTORY (DB + SHOP)
    // =====================================================================
    public static Game createFromConsole(ConsoleUI ui) {
        Random rnd = new Random();

        ui.println("PostgreSQL connection:");
        String url  = ui.readNonEmpty("JDBC URL (e.g. jdbc:postgresql://localhost:5432/perudo): ");
        String user = ui.readNonEmpty("DB user: ");
        String pass = ui.readNonEmpty("DB password: ");

        PgAccountRepository accRepo = new PgAccountRepository(url, user, pass);
        PgBonusRepository bonusRepo = new PgBonusRepository(accRepo);

        int humans = ui.readInt("Humans (1..4): ", 1, 4);
        int bots = ui.readInt("Bots (0..5): ", 0, 5);

        List<Player> players = new ArrayList<>();

        // Humans: choose account -> optional shop -> build wallet from DB inventory
        for (int i = 1; i <= humans; i++) {
            ui.println("\nSelect account for Human " + i + ":");
            Account acc = pickAccount(ui, accRepo);

            ui.println("Enter shop before game? [Y/N]");
            String ans = ui.readLine().trim().toUpperCase();
            if (ans.startsWith("Y")) {
                ShopService.openShop(ui, accRepo, bonusRepo, acc);
            }

            // Reload account + inventory after shop
            Account fresh = accRepo.findByUsername(acc.getUsername());
            Map<String, Integer> inv = bonusRepo.getInventory(fresh.getId());
            PlayerWallet wallet = new PlayerWallet(fresh.getId(), inv);

            players.add(new HumanPlayer(fresh.getUsername(), new DiceCup(5, rnd), wallet));
        }

        // Bots
        BotStrategy strategy = new SimpleBotStrategy(rnd);
        for (int i = 1; i <= bots; i++) {
            players.add(new BotPlayer("Bot" + i, new DiceCup(5, rnd), strategy));
        }

        RuleEngine rules = new RuleEngine(5); // max dice (for exact reward cap)
        int startIndex = rnd.nextInt(players.size());
        return new Game(players, ui, rules, bonusRepo, startIndex);
    }

    private static Account pickAccount(ConsoleUI ui, PgAccountRepository repo) {
        while (true) {
            List<Account> accounts = repo.findAll();

            if (accounts.isEmpty()) {
                ui.println("No accounts in DB. Create the first one.");
                String name = ui.readNonEmpty("New username: ");
                return repo.createIfNotExists(name);
            }

            ui.println("Accounts:");
            for (int i = 0; i < accounts.size(); i++) {
                ui.println("  " + (i + 1) + ") " + accounts.get(i).getUsername() + " (coins: " + accounts.get(i).getCoins() + ")");
            }
            ui.println("  " + (accounts.size() + 1) + ") Create new account");

            int choice = ui.readInt("Choose: ", 1, accounts.size() + 1);

            if (choice == accounts.size() + 1) {
                String name = ui.readNonEmpty("New username: ");
                return repo.createIfNotExists(name);
            }

            return accounts.get(choice - 1);
        }
    }
}
