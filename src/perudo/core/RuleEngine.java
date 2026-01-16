package perudo.core;

import perudo.players.Player;
import perudo.ui.ConsoleUI;

public final class RuleEngine {
    private final int maxDice;

    public RuleEngine(int maxDice) {
        this.maxDice = maxDice;
    }

    public void resolveLiar(RoundContext ctx, int callerIndex, ConsoleUI ui) {
        Bid bid = ctx.currentBid();
        int actual = ctx.countMatchesTotal(bid);

        ui.println("\n>>> LIAR called!");
        ui.println("Bid: " + bid + " | Actual matches: " + actual);

        int bidderIndex = ctx.lastBidderIndex();
        boolean bidTrue = actual >= bid.quantity();

        if (bidTrue) {
            ui.println("Bid is TRUE. Caller loses 1 die.");
            ctx.players().get(callerIndex).loseDie();
            ctx.setNextStarterIndex(callerIndex);
        } else {
            ui.println("Bid is FALSE. Bidder loses 1 die.");
            ctx.players().get(bidderIndex).loseDie();
            ctx.setNextStarterIndex(bidderIndex);
        }

        printCounts(ctx, ui);
    }

    public void resolveExact(RoundContext ctx, int callerIndex, ConsoleUI ui) {
        Bid bid = ctx.currentBid();
        int actual = ctx.countMatchesTotal(bid);

        ui.println("\n>>> EXACT called!");
        ui.println("Bid: " + bid + " | Actual matches: " + actual);

        int bidderIndex = ctx.lastBidderIndex();

        if (actual == bid.quantity()) {
            ui.println("Exactly correct! Bidder gains 1 die (up to max).");
            Player bidder = ctx.players().get(bidderIndex);
            // maxDice enforcement is in Player via cup.maxDice(), but keep it consistent:
            bidder.gainDieUpToMax();
            ctx.setNextStarterIndex(callerIndex);
        } else {
            ui.println("Not exact. Exact-caller loses 1 die.");
            ctx.players().get(callerIndex).loseDie();
            ctx.setNextStarterIndex(callerIndex);
        }

        printCounts(ctx, ui);
    }

    private void printCounts(RoundContext ctx, ConsoleUI ui) {
        ui.println("Dice counts:");
        for (Player p : ctx.players()) {
            ui.println(" - " + p.name() + ": " + p.diceCount());
        }
    }
}