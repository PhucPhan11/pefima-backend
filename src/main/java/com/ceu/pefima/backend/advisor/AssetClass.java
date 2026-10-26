package com.ceu.pefima.backend.advisor;

/**
 * Asset classes of the profile. The frontend sorts wallets into them with its
 * own rules; debts (negative wallets) are not a class.
 */
public enum AssetClass {

    /** Cash, bank accounts and savings books. */
    SAFE("Tiền mặt & tiết kiệm"),

    GOLD("Vàng"),

    FX("Ngoại tệ"),

    /** Open-ended funds. */
    FUND("Quỹ mở"),

    ETF("ETF"),

    STOCK("Cổ phiếu"),

    CRYPTO("Crypto"),

    /** Investments named by the user's local rule; compared with scenarios as {@link #FUND}. */
    OTHER("Đầu tư khác");

    private final String label;

    AssetClass(String label) {
        this.label = label;
    }

    public String label() {
        return this.label;
    }

    /** The class scenarios use for this one: {@link #OTHER} counts as {@link #FUND}. */
    AssetClass scenarioClass() {
        return this == OTHER ? FUND : this;
    }

}
