package perudo.accounts;

public class Account {
    private final long id;
    private final String username;
    private final int coins;
    public Account(long id, String username, int coins){
        if(username == null || username.trim().isEmpty()){
            throw new IllegalArgumentException("username cannot be empty");
        }
        this.id = id;
        this.username = username.trim();
        this.coins = coins;
    }
    public long getId(){
        return id;
    }
    public String getUsername(){
        return username;
    }
    public int getCoins(){
        return coins;
    }

    @Override
    public String toString() {
        return username;
    }
}
