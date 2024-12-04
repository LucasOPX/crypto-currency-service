package com.example.crypto.exchange.service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyRatesResponse {
    private String source;
    private Map<String, BigDecimal> rates;
}
