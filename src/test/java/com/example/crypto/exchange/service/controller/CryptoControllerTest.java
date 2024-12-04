package com.example.crypto.exchange.service.controller;

import com.example.crypto.exchange.service.model.CurrencyRatesResponse;
import com.example.crypto.exchange.service.model.ExchangeRequest;
import com.example.crypto.exchange.service.model.ExchangeResponse;
import com.example.crypto.exchange.service.service.CryptoRateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;


import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;


import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CryptoController.class)
class CryptoControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    CryptoRateService cryptoRateService;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        when(cryptoRateService.getFilteredRates("BTC", null))
                .thenReturn(new CurrencyRatesResponse("BTC", Map.of("USD", BigDecimal.valueOf(20000))));

        when(cryptoRateService.exchangeCurrencies("BTC", List.of("ETH"), BigDecimal.valueOf(100)))
                .thenReturn(new ExchangeResponse("BTC",
                        Map.of("ETH", new ExchangeResponse.ExchangeResult(
                                BigDecimal.TEN, BigDecimal.valueOf(100), BigDecimal.valueOf(950), BigDecimal.valueOf(1))
                        )));
    }

    @Test
    void testGetRates() throws Exception {
        mockMvc.perform(get("/currencies/BTC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("BTC"))
                .andExpect(jsonPath("$.rates.USD").value(20000.0));
    }

    @Test
    void testExchange() throws Exception {
        ExchangeRequest request = new ExchangeRequest();
        request.setFrom("BTC");
        request.setTo(List.of("ETH"));
        request.setAmount(BigDecimal.valueOf(100));

        mockMvc.perform(post("/currencies/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("BTC"))
                .andExpect(jsonPath("$.conversions.ETH.rate").value(10.0));
    }
}
