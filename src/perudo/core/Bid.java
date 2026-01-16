package perudo.core;

public final class Bid {
    private final int quantity;
    private final int face; // 1..6

    public Bid(int quantity, int face) {
        if (quantity < 1) throw new IllegalArgumentException("quantity must be >= 1");
        if (face < 1 || face > 6) throw new IllegalArgumentException("face must be 1..6");
        this.quantity = quantity;
        this.face = face;
    }

    public int quantity() { return quantity; }
    public int face() { return face; }

    public boolean isHigherThan(Bid other) {
        if (this.quantity != other.quantity) return this.quantity > other.quantity;
        return this.face > other.face;
    }

    public Bid nextMinimumBid() {
        if (face < 6) return new Bid(quantity, face + 1);
        return new Bid(quantity + 1, 1);
    }

    @Override
    public String toString() {
        return quantity + " x " + face + "'s";
    }
}

