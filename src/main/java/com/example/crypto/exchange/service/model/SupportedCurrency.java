package com.example.crypto.exchange.service.model;

public enum SupportedCurrency {
    BTC("bitcoin", "btc"),
    ETH("ethereum", "eth"),
    USD("usd", "usd"),
    USDT("tether", "usdt");


    private final String coinId;
    private final String vsCurrency;

    SupportedCurrency(String coinId, String vsCurrency) {
        this.coinId = coinId;
        this.vsCurrency = vsCurrency;
    }

    public String getCoinId() {
        return coinId;
    }

    public String getVsCurrency() {
        return vsCurrency;
    }

    public static SupportedCurrency fromSymbol(String symbol) {
        for (SupportedCurrency c : values()) {
            if (c.name().equalsIgnoreCase(symbol)) {
                return c;
            }
        }
        throw new IllegalArgumentException("Unsupported currency symbol: " + symbol);
    }
}
