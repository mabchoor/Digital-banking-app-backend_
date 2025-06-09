package com.example.digitalbankingbackend.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreditRequestDTO(
        @NotBlank(message = "Account ID cannot be blank") String accountId,
        @NotNull(message = "Amount cannot be null") @Min(value = 1, message = "Amount must be positive") double amount,
        String description
) {}