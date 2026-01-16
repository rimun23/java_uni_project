
package perudo.core;

import perudo.players.BotPlayer;
import perudo.players.Player;

import java.util.List;

public final class RoundContext {
    private final List<Player> players;
    private Bid currentBid;
    private int lastBidderIndex = -1;
    private int nextStarterIndex = 0;

    public RoundContext(List<Player> players) {
        this.players = players;
    }

    public List<Player> players() { return players; }

    public Bid currentBid() { return currentBid; }
    public int lastBidderIndex() { return lastBidderIndex; }

    public void setBid(Bid bid, int bidderIndex) {
        this.currentBid = bid;
        this.lastBidderIndex = bidderIndex;
    }

    public int totalDiceInPlay() {
        int sum = 0;
        for (Player p : players) sum += p.diceCount();
        return sum;
    }
    public boolean hasAliveBots() {
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).isAlive() && (players.get(i) instanceof BotPlayer)) return true;
        }
        return false;
    }

    public java.util.List<Integer> aliveBotIndexes() {
        java.util.List<Integer> res = new java.util.ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).isAlive() && (players.get(i) instanceof BotPlayer)) res.add(i);
        }
        return res;
    }

    public int countMatchesTotal(Bid bid) {
        int face = bid.face();
        int count = 0;

        for (Player p : players) {
            if (!p.isAlive()) continue;
            for (int d : p.cup().dice()) {
                if (face == 1) {
                    if (d == 1) count++;
                } else {
                    if (d == face || d == 1) count++; // ones wild
                }
            }
        }
        return count;
    }

    public int countMatchesInCup(DiceCup cup, int face) {
        int count = 0;
        for (int d : cup.dice()) {
            if (face == 1) {
                if (d == 1) count++;
            } else {
                if (d == face || d == 1) count++;
            }
        }
        return count;
    }

    public void setNextStarterIndex(int idx) { this.nextStarterIndex = idx; }
    public int nextStarterIndex() { return nextStarterIndex; }
}