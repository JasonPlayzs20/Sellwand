package basementhost.randomchad.service;

public record SellResult(Status status, int amountSold, double money) {
    public enum Status {
        SOLD,
        NOTHING_TO_SELL,
        VAULT_FAILED
    }

    public static SellResult sold(int amountSold, double money) {
        return new SellResult(Status.SOLD, amountSold, money);
    }

    public static SellResult nothingToSell() {
        return new SellResult(Status.NOTHING_TO_SELL, 0, 0D);
    }

    public static SellResult vaultFailed() {
        return new SellResult(Status.VAULT_FAILED, 0, 0D);
    }
}
