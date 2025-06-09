package com.example.digitalbankingbackend.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TransferRequestDTO(
        @NotBlank(message = "Source Account ID cannot be blank") String accountSource,
        @NotBlank(message = "Destination Account ID cannot be blank") String accountDestination,
        @NotNull(message = "Amount cannot be null") @Min(value = 1, message = "Amount must be positive") double amount
) {}