import perudo.core.Game;
import perudo.ui.ConsoleUI;

public class Main {
    public static void main(String[] args) {
        ConsoleUI ui = new ConsoleUI();
        Game game = Game.createFromConsole(ui);
        game.play();
    }
}
