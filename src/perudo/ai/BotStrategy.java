package perudo.ai;

import perudo.core.Action;
import perudo.core.RoundContext;
import perudo.players.Player;

public interface BotStrategy {
    Action choose(Player self, RoundContext ctx);
}
