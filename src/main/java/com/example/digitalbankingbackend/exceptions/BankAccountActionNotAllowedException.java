package com.example.digitalbankingbackend.exceptions;

public class BankAccountActionNotAllowedException extends Exception {
    public BankAccountActionNotAllowedException(String message) {
        super(message);
    }
}