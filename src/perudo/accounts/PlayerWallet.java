package perudo.accounts;
import perudo.core.BonusKinds;
import java.util.Map;
public class PlayerWallet {
    private final long accountId;
    private int rerollCount;
    private int peekCount;
    private boolean rerollUsedThisGame = false;
    private boolean peekUsedThisGame = false;
    public PlayerWallet(long accountId, Map<String, Integer> inventory){
        this.accountId = accountId;
        this.rerollCount = inventory.getOrDefault(BonusKinds.reroll, 0);
        this.peekCount = inventory.getOrDefault(BonusKinds.peek, 0);
    }
    public long getAccountId(){
        return accountId;
    }
    public int getRerollCount() {
        return rerollCount;
    }
    public int getPeekCount() {
        return peekCount;
    }
    public boolean canUseReroll(){
        return rerollCount > 0 && !rerollUsedThisGame;
    }
    public boolean canUsePeek(){
        return peekCount > 0 && !peekUsedThisGame;
    }
    public void markRerollUsed(){
        rerollUsedThisGame = true;
    }
    public void markPeekUsed(){
        peekUsedThisGame = true;
    }
    public void decrementRerollLocal() { rerollCount = Math.max(0, rerollCount - 1); }
    public void decrementPeekLocal() { peekCount = Math.max(0, peekCount - 1); }
}
