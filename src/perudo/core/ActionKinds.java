package perudo.core;

public final class ActionKinds {
    private ActionKinds() {}

    public static final int BID = 1;
    public static final int LIAR = 2;
    public static final int EXACT = 3;

    // bonuses (do not end the round)
    public static final int BONUS_REROLL = 10;
    public static final int BONUS_PEEK = 11;
}
