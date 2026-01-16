package perudo.players;

import perudo.ai.BotStrategy;
import perudo.core.*;
import perudo.ui.ConsoleUI;

public final class BotPlayer extends Player {
    private final BotStrategy strategy;

    public BotPlayer(String name, DiceCup cup, BotStrategy strategy) {
        super(name, cup);
        this.strategy = strategy;
    }

    @Override
    public Action chooseAction(perudo.core.RoundContext ctx, ConsoleUI ui) {
        return strategy.choose(this, ctx);
    }
}

