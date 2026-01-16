package perudo.players;

import perudo.core.Action;
import perudo.core.DiceCup;
import perudo.ui.ConsoleUI;

public abstract class Player {
    private final String name;
    private final DiceCup cup;
    private int diceCount;

    // one-time bonuses per game
    private boolean rerollUsed = false;
    private boolean peekUsed = false;

    protected Player(String name, DiceCup cup) {
        this.name = name;
        this.cup = cup;
        this.diceCount = cup.maxDice();
    }

    public abstract Action chooseAction(perudo.core.RoundContext ctx, ConsoleUI ui);

    public void roll() {
        cup.roll(diceCount);
    }

    public boolean isAlive() { return diceCount > 0; }

    public void loseDie() { diceCount = Math.max(0, diceCount - 1); }

    public void gainDieUpToMax() { diceCount = Math.min(cup.maxDice(), diceCount + 1); }

    public String name() { return name; }
    public DiceCup cup() { return cup; }
    public int diceCount() { return diceCount; }

    // ===== bonuses =====
    public boolean canReroll() { return !rerollUsed && isAlive(); }
    public boolean canPeek() { return !peekUsed && isAlive(); }

    public void useReroll() { rerollUsed = true; }
    public void usePeek() { peekUsed = true; }

    public boolean rerollUsed() { return rerollUsed; }
    public boolean peekUsed() { return peekUsed; }
}