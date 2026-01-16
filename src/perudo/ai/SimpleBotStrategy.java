package perudo.ai;

import perudo.core.*;
import perudo.players.Player;

import java.util.Random;

public final class SimpleBotStrategy implements BotStrategy {
    private final Random rnd;

    public SimpleBotStrategy(Random rnd) {
        this.rnd = rnd;
    }

    @Override
    public Action choose(Player self, RoundContext ctx) {
        Bid cur = ctx.currentBid();

        if (cur == null) {
            int face = (rnd.nextDouble() < 0.10) ? 1 : (2 + rnd.nextInt(5));
            return Action.bid(new Bid(1, face));
        }

        int totalDice = ctx.totalDiceInPlay();
        int myDice = self.diceCount();
        int others = totalDice - myDice;

        int face = cur.face();
        int myMatches = ctx.countMatchesInCup(self.cup(), face);

        double expectedOthers = (face == 1) ? (others / 6.0) : (others / 3.0);
        double expectedTotal = myMatches + expectedOthers;

        if (cur.quantity() > expectedTotal + 1.6) return Action.liar();

        if (Math.abs(cur.quantity() - expectedTotal) < 0.7 && rnd.nextDouble() < 0.15) {
            return Action.exact();
        }

        Bid next = cur.nextMinimumBid();
        return Action.bid(next);
    }
}
