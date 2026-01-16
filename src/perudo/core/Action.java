package perudo.core;

public final class Action {
    private final int kind;
    private final Bid bid;      // only for BID
    private final int target;   // only for PEEK (bot index), else -1

    private Action(int kind, Bid bid, int target) {
        this.kind = kind;
        this.bid = bid;
        this.target = target;
    }

    public static Action bid(Bid bid) {
        if (bid == null) throw new IllegalArgumentException("bid cannot be null");
        return new Action(ActionKinds.BID, bid, -1);
    }

    public static Action liar() {
        return new Action(ActionKinds.LIAR, null, -1);
    }

    public static Action exact() {
        return new Action(ActionKinds.EXACT, null, -1);
    }

    public static Action bonusReroll() {
        return new Action(ActionKinds.BONUS_REROLL, null, -1);
    }

    public static Action bonusPeek(int targetPlayerIndex) {
        return new Action(ActionKinds.BONUS_PEEK, null, targetPlayerIndex);
    }

    public int kind() { return kind; }
    public Bid bid() { return bid; }
    public int target() { return target; }
}

