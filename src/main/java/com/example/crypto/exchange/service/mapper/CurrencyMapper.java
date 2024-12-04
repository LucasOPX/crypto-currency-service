package com.example.crypto.exchange.service.mapper;

import com.example.crypto.exchange.service.model.SupportedCurrency;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CurrencyMapper {

    private CurrencyMapper(){
    }

    public static String mapSymbolToId(String symbol, boolean isSource) {
        try {
            SupportedCurrency currency = SupportedCurrency.fromSymbol(symbol);
            return isSource ? currency.getCoinId() : currency.getVsCurrency();
        } catch (IllegalArgumentException e) {
            log.warn("Unknown currency symbol received: {}", symbol);
            throw e;
        }
    }
}
