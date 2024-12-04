package com.example.crypto.exchange.service.service;

import com.example.crypto.exchange.service.exception.ResourceNotFoundException;
import com.example.crypto.exchange.service.mapper.CurrencyMapper;
import com.example.crypto.exchange.service.model.CurrencyRatesResponse;
import com.example.crypto.exchange.service.model.ExchangeResponse;
import com.example.crypto.exchange.service.model.SupportedCurrency;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CryptoRateService {

    private final WebClient webClient;

    @Value("${app.cryptoApi.baseUrl}")
    private String baseUrl;

    @Getter
    @Value("${app.fee.percentage:0.01}")
    private BigDecimal feePercentage;

    public CryptoRateService(WebClient webClient) {
        this.webClient = webClient;
    }

    public CurrencyRatesResponse getFilteredRates(String currencySymbol, List<String> filters) {
        String sourceForApi = CurrencyMapper.mapSymbolToId(currencySymbol, true);
        log.debug("Fetching filtered rates for currency={}, filters={}", currencySymbol, filters);

        String[] targetArray = null;
        if (filters != null && !filters.isEmpty()) {
            targetArray = filters.stream()
                    .map(f -> CurrencyMapper.mapSymbolToId(f, false))
                    .toArray(String[]::new);
        }

        Map<String, BigDecimal> allRates = getRates(sourceForApi, targetArray);

        if (filters != null && !filters.isEmpty()) {
            Set<String> allowed = filters.stream()
                    .map(String::toUpperCase)
                    .collect(Collectors.toSet());

            allRates = allRates.entrySet().stream()
                    .filter(e -> allowed.contains(e.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        return new CurrencyRatesResponse(currencySymbol.toUpperCase(), allRates);
    }

    public ExchangeResponse exchangeCurrencies(String fromSymbol, List<String> toSymbols, BigDecimal amount) {
        log.info("Starting currency exchange (multi-threaded): from={}, to={}, amount={}", fromSymbol, toSymbols, amount);
        String fromForApi = CurrencyMapper.mapSymbolToId(fromSymbol, true);
        String[] toForApi = toSymbols.stream().map(t -> CurrencyMapper.mapSymbolToId(t, false)).toArray(String[]::new);

        Map<String, BigDecimal> rates = getRates(fromForApi, toForApi);
        log.debug("Rates retrieved: {}, feePercentage={}", rates, feePercentage);

        List<CompletableFuture<Map.Entry<String, ExchangeResponse.ExchangeResult>>> futures = new ArrayList<>();

        for (int i = 0; i < toSymbols.size(); i++) {
            String toSymbol = toSymbols.get(i);
            String toId = toForApi[i].toUpperCase();

            CompletableFuture<Map.Entry<String, ExchangeResponse.ExchangeResult>> future = CompletableFuture.supplyAsync(() -> {
                BigDecimal rate = rates.get(toId);
                if (rate == null) {
                    log.warn("No rate found for conversion from {} to {}", fromSymbol, toSymbol);
                    return null;
                }

                BigDecimal fee = amount.multiply(feePercentage);
                BigDecimal amountAfterFee = amount.subtract(fee);
                BigDecimal result = amountAfterFee.multiply(rate);

                log.debug("Calculated exchange in thread: from={} to={}, rate={}, fee={}, amountAfterFee={}, result={}",
                        fromSymbol, toSymbol, rate, fee, amountAfterFee, result);

                ExchangeResponse.ExchangeResult exResult = new ExchangeResponse.ExchangeResult(rate, amount, result, fee);
                return Map.entry(toSymbol.toUpperCase(), exResult);
            });

            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        Map<String, ExchangeResponse.ExchangeResult> conversions = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        ExchangeResponse response = new ExchangeResponse(fromSymbol.toUpperCase(), conversions);
        log.info("Exchange completed (multi-threaded) for from={}, to={}, final response={}", fromSymbol, toSymbols, response);
        return response;
    }

    public Map<String, BigDecimal> getRates(String source, String[] targets) {
        if (targets == null || targets.length == 0) {
            targets = Arrays.stream(SupportedCurrency.values())
                    .map(SupportedCurrency::getVsCurrency)
                    .toArray(String[]::new);
        }

        String vsCurrencies = String.join(",", targets);
        log.debug("Fetching rates for source={} vs={}", source, vsCurrencies);

        Map<String, Map<String, Double>> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("api.coingecko.com")
                        .path("/api/v3/simple/price")
                        .queryParam("ids", source)
                        .queryParam("vs_currencies", vsCurrencies)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey(source)) {
            log.warn("No data returned from API for source={}", source);
            throw new ResourceNotFoundException("Currency data not found for: " + source);
        }

        Map<String, BigDecimal> rates = response.get(source).entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().toUpperCase(),
                        e -> {
                            Object val = e.getValue();
                            if (val instanceof Number number) {
                                return BigDecimal.valueOf(number.doubleValue());
                            } else {
                                log.error("API returned a non-numeric value: {}", val);
                                throw new ResourceNotFoundException("Non-numeric rate value received from API");
                            }
                        }
                ));

        log.debug("Converted rates: {}", rates);
        return rates;
    }
}
