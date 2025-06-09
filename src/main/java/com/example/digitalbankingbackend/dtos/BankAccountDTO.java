package com.example.digitalbankingbackend.dtos;

import com.example.digitalbankingbackend.enums.AccountStatus;
import lombok.Data;
import java.util.Date;

@Data
public class BankAccountDTO {
    private String type;
    private String id;
    private double balance;
    private Date createdAt;
    private AccountStatus status;
    private CustomerDTO customerDTO;
}