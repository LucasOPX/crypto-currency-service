package com.example.crypto.exchange.service.controller;

import com.example.crypto.exchange.service.model.CurrencyRatesResponse;
import com.example.crypto.exchange.service.model.ExchangeRequest;
import com.example.crypto.exchange.service.model.ExchangeResponse;
import com.example.crypto.exchange.service.service.CryptoRateService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/currencies")
@Slf4j
public class CryptoController {

    private final CryptoRateService cryptoRateService;

    public CryptoController(CryptoRateService cryptoRateService) {
        this.cryptoRateService = cryptoRateService;
    }

    @GetMapping("/{currency}")
    public CurrencyRatesResponse getRates(
            @PathVariable("currency") String currency,
            @RequestParam(name="filter[]", required = false) List<String> filters) {
        log.info("Received request to get rates for currency={} with filters={}", currency, filters);
        CurrencyRatesResponse response = cryptoRateService.getFilteredRates(currency, filters);
        log.info("Returning rates for {}: {}", currency, response.getRates());
        return response;
    }

    @PostMapping("/exchange")
    public ExchangeResponse exchange(@RequestBody @Valid ExchangeRequest request) {
        log.info("Received exchange request: {}", request);
        ExchangeResponse response = cryptoRateService.exchangeCurrencies(request.getFrom(), request.getTo(), request.getAmount());
        log.info("Exchange request processed successfully: {}", response);
        return response;
    }
}
