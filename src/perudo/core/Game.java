package perudo.core;

import perudo.accounts.*;
import perudo.ai.*;
import perudo.db.DbConfig;
import perudo.players.*;
import perudo.ui.ConsoleUI;

import java.util.*;

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

        for (Player p : players) {
            if (p.isAlive()) p.roll();
        }

        for (Player p : players) {
            if (p.isAlive() && p instanceof HumanPlayer) {
                ui.println(p.name() + " dice: " + p.cup().sortedString());
            }
        }

        int turn = currentIndex;

        boolean repeatSamePlayer = false;

        while (true) {
            if (!repeatSamePlayer) {
                turn = nextAliveIndex(turn);
            } else {
                repeatSamePlayer = false;
            }

            Player p = players.get(turn);

            ui.println("\nTurn: " + p.name() + " (dice: " + p.diceCount() + ")");
            ui.println("Current bid: " + (ctx.currentBid() == null ? "none" : ctx.currentBid()));

            Action action = p.chooseAction(ctx, ui);
            
            if (action.kind() == ActionKinds.BONUS_REROLL) {
                if (!(p instanceof HumanPlayer)) {
                    ui.println("Only humans can use bonuses.");
                    repeatSamePlayer = true;
                    continue;
                }

                HumanPlayer hp = (HumanPlayer) p;
                PlayerWallet w = hp.wallet();

                if (!w.canUseReroll()) {
                    ui.println("REROLL not available (0 in inventory or already used this match).");
                    repeatSamePlayer = true;
                    continue;
                }

                boolean consumed = bonusRepo.consumeOne(w.getAccountId(), BonusKinds.reroll);
                if (!consumed) {
                    ui.println("REROLL not available in DB (inventory desync).");
                    repeatSamePlayer = true;
                    continue;
                }

                w.decrementRerollLocal();
                w.markRerollUsed();

                p.roll();
                ui.println(p.name() + " used REROLL. New dice: " + p.cup().sortedString());

                continue;
            }

            if (action.kind() == ActionKinds.BONUS_PEEK) {
                if (!(p instanceof HumanPlayer)) {
                    ui.println("Only humans can use bonuses.");
                    repeatSamePlayer = true;
                    continue;
                }

                if (!ctx.hasAliveBots()) {
                    ui.println("No alive bots to peek.");
                    repeatSamePlayer = true;
                    continue;
                }

                HumanPlayer hp = (HumanPlayer) p;
                PlayerWallet w = hp.wallet();

                if (!w.canUsePeek()) {
                    ui.println("PEEK not available (0 in inventory or already used this match).");
                    repeatSamePlayer = true;
                    continue;
                }

                int target = action.target();
                if (target < 0 || target >= ctx.players().size()) {
                    ui.println("Invalid peek target.");
                    repeatSamePlayer = true;
                    continue;
                }
                if (!ctx.players().get(target).isAlive() || !(ctx.players().get(target) instanceof BotPlayer)) {
                    ui.println("You can peek only an alive bot.");
                    repeatSamePlayer = true;
                    continue;
                }

                boolean consumed = bonusRepo.consumeOne(w.getAccountId(), BonusKinds.peek);
                if (!consumed) {
                    ui.println("PEEK not available in DB (inventory desync).");
                    repeatSamePlayer = true;
                    continue;
                }

                w.decrementPeekLocal();
                w.markPeekUsed();

                ui.println(p.name() + " used PEEK.");
                ui.println("Peek " + ctx.players().get(target).name() + " dice: " +
                        ctx.players().get(target).cup().sortedString());

                repeatSamePlayer = true;
                continue;
            }

            if (action.kind() == ActionKinds.BID) {
                Bid bid = action.bid();

                if (ctx.currentBid() != null && !bid.isHigherThan(ctx.currentBid())) {
                    if (p instanceof HumanPlayer) {
                        ui.println("Invalid bid. Must be higher than current bid.");
                        continue;
                    } else {
                        bid = ctx.currentBid().nextMinimumBid();
                    }
                }

                ctx.setBid(bid, turn);
                ui.println(p.name() + " bids: " + bid);
                continue;
            }

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

    public static Game createFromConsole(ConsoleUI ui) {
        Random rnd = new Random();

        PgAccountRepository accRepo = new PgAccountRepository(
                DbConfig.DB_URL,
                DbConfig.DB_USER,
                DbConfig.DB_PASSWORD
        );
        PgBonusRepository bonusRepo = new PgBonusRepository(accRepo);

        int humans = ui.readInt("Humans (1..4): ", 1, 4);
        int bots = ui.readInt("Bots (0..5): ", 0, 5);

        List<Player> players = new ArrayList<>();

        Set<String> usedUsernames = new HashSet<>();

        for (int i = 1; i <= humans; i++) {
            ui.println("\nSelect account for Human " + i + ":");
            Account acc = pickAccount(ui, accRepo, usedUsernames);
            usedUsernames.add(acc.getUsername());

            ui.println("Enter shop before game? [Y/N]");
            String ans = ui.readLine().trim().toUpperCase();
            if (ans.startsWith("Y")) {
                ShopService.openShop(ui, accRepo, bonusRepo, acc);
            }

            Account fresh = accRepo.findByUsername(acc.getUsername());
            Map<String, Integer> inv = bonusRepo.getInventory(fresh.getId());
            PlayerWallet wallet = new PlayerWallet(fresh.getId(), inv);

            players.add(new HumanPlayer(fresh.getUsername(), new DiceCup(5, rnd), wallet));
        }

        BotStrategy strategy = new SimpleBotStrategy(rnd);
        for (int i = 1; i <= bots; i++) {
            players.add(new BotPlayer("Bot" + i, new DiceCup(5, rnd), strategy));
        }

        RuleEngine rules = new RuleEngine(5);
        int startIndex = rnd.nextInt(players.size());
        return new Game(players, ui, rules, bonusRepo, startIndex);
    }

    private static Account pickAccount(ConsoleUI ui, PgAccountRepository repo, Set<String> usedUsernames) {
        while (true) {
            List<Account> all = repo.findAll();

            List<Account> available = new ArrayList<>();
            for (Account a : all) {
                if (!usedUsernames.contains(a.getUsername())) {
                    available.add(a);
                }
            }

            if (available.isEmpty()) {
                ui.println("All existing accounts already selected. Create a new one.");
                String name = ui.readNonEmpty("New username: ");
                if (usedUsernames.contains(name)) {
                    ui.println("This username is already selected in this match.");
                    continue;
                }
                return repo.createIfNotExists(name);
            }

            ui.println("Accounts (available):");
            for (int i = 0; i < available.size(); i++) {
                Account a = available.get(i);
                ui.println("  " + (i + 1) + ") " + a.getUsername() + " (coins: " + a.getCoins() + ")");
            }
            ui.println("  " + (available.size() + 1) + ") Create new account");

            int choice = ui.readInt("Choose: ", 1, available.size() + 1);

            if (choice == available.size() + 1) {
                String name = ui.readNonEmpty("New username: ");
                if (usedUsernames.contains(name)) {
                    ui.println("This username is already selected in this match.");
                    continue;
                }
                return repo.createIfNotExists(name);
            }

            return available.get(choice - 1);
        }
    }
}
