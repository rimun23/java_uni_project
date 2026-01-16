
package perudo.players;

import perudo.accounts.PlayerWallet;
import perudo.core.*;
import perudo.ui.ConsoleUI;

import java.util.List;

public final class HumanPlayer extends Player {
    private final PlayerWallet wallet;

    public HumanPlayer(String name, DiceCup cup, PlayerWallet wallet) {
        super(name, cup);
        this.wallet = wallet;
    }

    public PlayerWallet wallet() { return wallet; }

    @Override
    public Action chooseAction(perudo.core.RoundContext ctx, ConsoleUI ui) {
        while (true) {
            StringBuilder sb = new StringBuilder("Choose: [B]id");
            if (ctx.currentBid() != null) sb.append(", [L]iar, [E]xact");

            if (wallet.canUseReroll()) sb.append(", [R]eroll (have ").append(wallet.getRerollCount()).append(")");
            if (wallet.canUsePeek() && ctx.hasAliveBots()) sb.append(", [P]eek bot (have ").append(wallet.getPeekCount()).append(")");

            ui.println(sb.toString());

            String s = ui.readLine().trim().toUpperCase();
            if (s.isEmpty()) continue;

            char c = s.charAt(0);

            if (c == 'R' && wallet.canUseReroll()) return Action.bonusReroll();

            if (c == 'P' && wallet.canUsePeek() && ctx.hasAliveBots()) {
                List<Integer> botIndexes = ctx.aliveBotIndexes();
                ui.println("Choose bot to peek:");
                for (int i = 0; i < botIndexes.size(); i++) {
                    int idx = botIndexes.get(i);
                    ui.println("  " + (i + 1) + ") " + ctx.players().get(idx).name());
                }
                int choice = ui.readInt("Bot number: ", 1, botIndexes.size());
                int targetIndex = botIndexes.get(choice - 1);
                return Action.bonusPeek(targetIndex);
            }

            if (c == 'B') {
                int q = ui.readInt("Quantity: ", 1, 200);
                int f = ui.readInt("Face (1..6): ", 1, 6);
                return Action.bid(new Bid(q, f));
            }

            if (ctx.currentBid() != null && c == 'L') return Action.liar();
            if (ctx.currentBid() != null && c == 'E') return Action.exact();

            ui.println("Invalid choice.");
        }
    }
}
