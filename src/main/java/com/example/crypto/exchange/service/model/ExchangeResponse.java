package com.example.crypto.exchange.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeResponse {
    private String from;
    private Map<String, ExchangeResult> conversions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExchangeResult {
        private BigDecimal rate;
        private BigDecimal amount;
        private BigDecimal result;
        private BigDecimal fee;
    }
}
