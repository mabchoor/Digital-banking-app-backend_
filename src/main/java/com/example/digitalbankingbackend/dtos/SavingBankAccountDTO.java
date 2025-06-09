package com.example.digitalbankingbackend.dtos;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SavingBankAccountDTO extends BankAccountDTO {
    private double interestRate;
}