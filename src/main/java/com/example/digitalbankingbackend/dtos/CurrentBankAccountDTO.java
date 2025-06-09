package com.example.digitalbankingbackend.dtos;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CurrentBankAccountDTO extends BankAccountDTO {
    private double overDraft;
}