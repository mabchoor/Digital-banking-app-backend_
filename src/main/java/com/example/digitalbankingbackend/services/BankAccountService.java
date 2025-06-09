package com.example.digitalbankingbackend.services;

import com.example.digitalbankingbackend.dtos.*;
import com.example.digitalbankingbackend.enums.AccountStatus;
import com.example.digitalbankingbackend.exceptions.BalanceNotSufficientException;
import com.example.digitalbankingbackend.exceptions.BankAccountActionNotAllowedException;
import com.example.digitalbankingbackend.exceptions.BankAccountNotFoundException;
import com.example.digitalbankingbackend.exceptions.CustomerNotFoundException;

import java.util.List;

public interface BankAccountService {
    // Customer operations
    CustomerDTO saveCustomer(CustomerDTO customerDTO);
    CustomerDTO updateCustomer(CustomerDTO customerDTO) throws CustomerNotFoundException;
    void deleteCustomer(Long customerId) throws CustomerNotFoundException;
    List<CustomerDTO> listCustomers();
    CustomerDTO getCustomer(Long customerId) throws CustomerNotFoundException;
    List<CustomerDTO> searchCustomers(String keyword);

    // BankAccount operations
    CurrentBankAccountDTO saveCurrentBankAccount(double initialBalance, double overDraft, Long customerId) throws CustomerNotFoundException;
    SavingBankAccountDTO saveSavingBankAccount(double initialBalance, double interestRate, Long customerId) throws CustomerNotFoundException;
    BankAccountDTO getBankAccount(String accountId) throws BankAccountNotFoundException;
    List<BankAccountDTO> bankAccountList();
    List<BankAccountDTO> bankAccountList(boolean includeInactive);
    List<BankAccountDTO> getAccountsByCustomerId(Long customerId) throws CustomerNotFoundException;
    BankAccountDTO changeAccountStatus(String accountId, AccountStatus status) throws BankAccountNotFoundException;

    // Account operations
    void debit(String accountId, double amount, String description) throws BankAccountNotFoundException, BalanceNotSufficientException, BankAccountActionNotAllowedException;
    void credit(String accountId, double amount, String description) throws BankAccountNotFoundException, BankAccountActionNotAllowedException;
    void transfer(String accountIdSource, String accountIdDestination, double amount) throws BankAccountNotFoundException, BalanceNotSufficientException, BankAccountActionNotAllowedException;

    // History
    List<AccountOperationDTO> accountHistory(String accountId) throws BankAccountNotFoundException;
    AccountHistoryDTO getAccountHistory(String accountId, int page, int size) throws BankAccountNotFoundException;
}