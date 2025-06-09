package com.example.digitalbankingbackend.web;

import com.example.digitalbankingbackend.dtos.*;
import com.example.digitalbankingbackend.enums.AccountStatus;
import com.example.digitalbankingbackend.exceptions.BalanceNotSufficientException;
import com.example.digitalbankingbackend.exceptions.BankAccountActionNotAllowedException;
import com.example.digitalbankingbackend.exceptions.BankAccountNotFoundException;
import com.example.digitalbankingbackend.exceptions.CustomerNotFoundException;
import com.example.digitalbankingbackend.services.BankAccountService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@AllArgsConstructor
@Slf4j
@CrossOrigin("*")
public class BankAccountRestController {

    private final BankAccountService bankAccountService;

    @GetMapping("/{accountId}")
    public ResponseEntity<BankAccountDTO> getBankAccount(@PathVariable String accountId) throws BankAccountNotFoundException {
        log.info("REST request to get bank account by ID: {}", accountId);
        BankAccountDTO bankAccount = bankAccountService.getBankAccount(accountId);
        return ResponseEntity.ok(bankAccount);
    }

    @GetMapping
    public ResponseEntity<List<BankAccountDTO>> listBankAccounts(
            @RequestParam(name = "includeInactive", defaultValue = "false") boolean includeInactive) {
        log.info("REST request to list bank accounts. Include Inactive: {}", includeInactive);
        List<BankAccountDTO> accounts = bankAccountService.bankAccountList(includeInactive);
        return ResponseEntity.ok(accounts);
    }

    @PostMapping("/current")
    public ResponseEntity<CurrentBankAccountDTO> createCurrentAccount(
            @RequestParam double initialBalance,
            @RequestParam double overDraft,
            @RequestParam Long customerId) throws CustomerNotFoundException {
        log.info("REST request to create Current Account for customer ID: {} with balance: {} and overdraft: {}",
                customerId, initialBalance, overDraft);
        CurrentBankAccountDTO accountDTO = bankAccountService.saveCurrentBankAccount(initialBalance, overDraft, customerId);
        return new ResponseEntity<>(accountDTO, HttpStatus.CREATED);
    }

    @PostMapping("/saving")
    public ResponseEntity<SavingBankAccountDTO> createSavingAccount(
            @RequestParam double initialBalance,
            @RequestParam double interestRate,
            @RequestParam Long customerId) throws CustomerNotFoundException {
        log.info("REST request to create Saving Account for customer ID: {} with balance: {} and interest rate: {}",
                customerId, initialBalance, interestRate);
        SavingBankAccountDTO accountDTO = bankAccountService.saveSavingBankAccount(initialBalance, interestRate, customerId);
        return new ResponseEntity<>(accountDTO, HttpStatus.CREATED);
    }

    @GetMapping("/{accountId}/history")
    public ResponseEntity<List<AccountOperationDTO>> getAccountFullHistory(@PathVariable String accountId) throws BankAccountNotFoundException {
        log.info("REST request to get full operation history for account ID: {}", accountId);
        List<AccountOperationDTO> history = bankAccountService.accountHistory(accountId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{accountId}/pageHistory")
    public ResponseEntity<AccountHistoryDTO> getAccountPaginatedHistory(
            @PathVariable String accountId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size) throws BankAccountNotFoundException {
        log.info("REST request for paginated history. Account: {}, Page: {}, Size: {}", accountId, page, size);
        AccountHistoryDTO paginatedHistory = bankAccountService.getAccountHistory(accountId, page, size);
        return ResponseEntity.ok(paginatedHistory);
    }

    @PostMapping("/debit")
    public ResponseEntity<Void> debitAccount(@Valid @RequestBody DebitRequestDTO debitRequestDTO)
            throws BankAccountNotFoundException, BalanceNotSufficientException, BankAccountActionNotAllowedException {
        log.info("REST request to debit account: {}", debitRequestDTO.accountId());
        bankAccountService.debit(
                debitRequestDTO.accountId(),
                debitRequestDTO.amount(),
                debitRequestDTO.description());
        return ResponseEntity.ok().build(); 
    }

    @PostMapping("/credit")
    public ResponseEntity<Void> creditAccount(@Valid @RequestBody CreditRequestDTO creditRequestDTO)
            throws BankAccountNotFoundException, BankAccountActionNotAllowedException {
        log.info("REST request to credit account: {}", creditRequestDTO.accountId());
        bankAccountService.credit(
                creditRequestDTO.accountId(),
                creditRequestDTO.amount(),
                creditRequestDTO.description());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/transfer")
    public ResponseEntity<Void> transferFunds(@Valid @RequestBody TransferRequestDTO transferRequestDTO)
            throws BankAccountNotFoundException, BalanceNotSufficientException, BankAccountActionNotAllowedException {
        log.info("REST request to transfer from {} to {}", transferRequestDTO.accountSource(), transferRequestDTO.accountDestination());
        bankAccountService.transfer(
                transferRequestDTO.accountSource(),
                transferRequestDTO.accountDestination(),
                transferRequestDTO.amount());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{accountId}/status")
    public ResponseEntity<BankAccountDTO> changeAccountStatus(
            @PathVariable String accountId,
            @RequestParam AccountStatus status) throws BankAccountNotFoundException {
        log.info("REST request to change status of account {} to {}", accountId, status);
        BankAccountDTO updatedAccount = bankAccountService.changeAccountStatus(accountId, status);
        return ResponseEntity.ok(updatedAccount);
    }
}