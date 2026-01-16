package perudo.core;

import java.util.*;

public final class DiceCup {
    private final int maxDice;
    private final Random rnd;
    private final List<Integer> dice = new ArrayList<>();

    public DiceCup(int maxDice, Random rnd) {
        this.maxDice = maxDice;
        this.rnd = rnd;
    }

    public int maxDice() { return maxDice; }

    public void roll(int count) {
        dice.clear();
        for (int i = 0; i < count; i++) {
            dice.add(1 + rnd.nextInt(6));
        }
    }

    public List<Integer> dice() {
        return Collections.unmodifiableList(dice);
    }

    public String sortedString() {
        List<Integer> copy = new ArrayList<>(dice);
        Collections.sort(copy);
        return copy.toString();
    }
}
