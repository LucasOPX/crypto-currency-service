package com.example.crypto.exchange.service.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
public class ExchangeRequest {

    @NotNull(message = "Source currency 'from' cannot be null")
    @NotEmpty(message = "Source currency 'from' cannot be empty")
    private String from;

    @NotNull(message = "'to' list cannot be null")
    @NotEmpty(message = "'to' list cannot be empty")
    private List<@NotEmpty String> to;

    @Min(value = 1, message = "Amount must be at least 1")
    private BigDecimal amount;
}
