package com.example.crypto.exchange.service.service;

import com.example.crypto.exchange.service.exception.ResourceNotFoundException;
import com.example.crypto.exchange.service.model.ExchangeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class CryptoRateServiceTest {

    private CryptoRateService cryptoRateService;
    private WebClient mockWebClient;
    private WebClient.RequestHeadersUriSpec uriSpec;
    private WebClient.RequestHeadersSpec headersSpec;
    private WebClient.ResponseSpec responseSpec;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        mockWebClient = Mockito.mock(WebClient.class);
        uriSpec = Mockito.mock(WebClient.RequestHeadersUriSpec.class);
        headersSpec = Mockito.mock(WebClient.RequestHeadersSpec.class);
        responseSpec = Mockito.mock(WebClient.ResponseSpec.class);

        when(mockWebClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri((Function<UriBuilder, URI>)any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        cryptoRateService = new CryptoRateService(mockWebClient);
        ReflectionTestUtils.setField(cryptoRateService, "feePercentage", BigDecimal.valueOf(0.01));
    }

    @Test
    void testGetRatesSuccessfulResponse() {
        Map<String, Object> mockResponse = Map.of(
                "bitcoin", Map.of("usd", 20000.0, "eth", 10.0)
        );

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(mockResponse));

        Map<String, BigDecimal> rates = cryptoRateService.getRates("bitcoin", new String[]{"usd", "eth"});
        assertEquals(BigDecimal.valueOf(20000.0), rates.get("USD"));
        assertEquals(BigDecimal.valueOf(10.0), rates.get("ETH"));
    }

    @Test
    void testGetRatesNoData() {
        Map<String, Object> mockResponse = Map.of();
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(mockResponse));

        assertThrows(ResourceNotFoundException.class, () ->
                cryptoRateService.getRates("bitcoin", new String[]{"usd"})
        );
    }

    @Test
    void testGetRatesNonNumericValue() {
        Map<String, Object> mockResponse = Map.of(
                "bitcoin", Map.of("usd", "non-numeric")
        );
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(mockResponse));

        assertThrows(ResourceNotFoundException.class, () ->
                cryptoRateService.getRates("bitcoin", new String[]{"usd"})
        );
    }

    @Test
    void testExchangeCurrenciesPartialSuccess() {
        Map<String, Object> mockResponse = Map.of(
                "bitcoin", Map.of("eth", 10.0) // USDT missing
        );
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(mockResponse));

        ExchangeResponse response = cryptoRateService.exchangeCurrencies("BTC", List.of("ETH","USDT"), BigDecimal.valueOf(100));
        assertTrue(response.getConversions().containsKey("ETH"));
        assertFalse(response.getConversions().containsKey("USDT"));
    }
}
