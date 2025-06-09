package com.example.digitalbankingbackend.services;

import com.example.digitalbankingbackend.dtos.*;
import com.example.digitalbankingbackend.entities.*;
import com.example.digitalbankingbackend.enums.AccountStatus;
import com.example.digitalbankingbackend.enums.OperationType;
import com.example.digitalbankingbackend.exceptions.BalanceNotSufficientException;
import com.example.digitalbankingbackend.exceptions.BankAccountActionNotAllowedException;
import com.example.digitalbankingbackend.exceptions.BankAccountNotFoundException;
import com.example.digitalbankingbackend.exceptions.CustomerNotFoundException;
import com.example.digitalbankingbackend.mappers.BankAccountMapper;
import com.example.digitalbankingbackend.repositories.AccountOperationRepository;
import com.example.digitalbankingbackend.repositories.BankAccountRepository;
import com.example.digitalbankingbackend.repositories.CustomerRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@AllArgsConstructor
@Slf4j
public class BankAccountServiceImpl implements BankAccountService {

    private final CustomerRepository customerRepository;
    private final BankAccountRepository bankAccountRepository;
    private final AccountOperationRepository accountOperationRepository;
    private final BankAccountMapper dtoMapper;

    @Override
    public CustomerDTO saveCustomer(CustomerDTO customerDTO) {
        log.info("Saving new Customer: {}", customerDTO.getName());
        Customer customer = dtoMapper.fromCustomerDTO(customerDTO);
        Customer savedCustomer = customerRepository.save(customer);
        return dtoMapper.fromCustomer(savedCustomer);
    }

    @Override
    public CustomerDTO updateCustomer(CustomerDTO customerDTO) throws CustomerNotFoundException {
        log.info("Updating Customer ID: {}", customerDTO.getId());
        Customer existingCustomer = customerRepository
                .findById(customerDTO.getId())
                .orElseThrow(() -> new CustomerNotFoundException("Customer Not found with ID: " + customerDTO.getId()));

        existingCustomer.setName(customerDTO.getName());
        existingCustomer.setEmail(customerDTO.getEmail());

        Customer updatedCustomer = customerRepository.save(existingCustomer);
        return dtoMapper.fromCustomer(updatedCustomer);
    }    @Override
    public void deleteCustomer(Long customerId) throws CustomerNotFoundException {
        log.warn("Attempting to delete Customer ID: {}", customerId);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer with ID " + customerId + " not found."));

        List<BankAccount> customerBankAccounts = bankAccountRepository.findByCustomerId(customerId);

        if (customerBankAccounts != null && !customerBankAccounts.isEmpty()) {
            log.info("Soft deleting {} bank accounts for customer ID: {}", customerBankAccounts.size(), customerId);
            for (BankAccount account : customerBankAccounts) {
                account.setStatus(AccountStatus.SUSPENDED);
                account.setCustomer(null);
            }
            bankAccountRepository.saveAllAndFlush(customerBankAccounts);
        } else {
            log.info("No bank accounts found for customer ID: {} to soft delete.", customerId);
        }

        log.info("Physically deleting customer record for ID: {}", customerId);
        customerRepository.delete(customer);
    }

    private AccountStatus determineInitialAccountStatus(Customer customer) {
        List<BankAccount> existingAccounts = bankAccountRepository.findByCustomerId(customer.getId());

        long activeAccountCount = existingAccounts.stream()
                .filter(acc -> acc.getStatus() == AccountStatus.ACTIVATED || acc.getStatus() == AccountStatus.CREATED)
                .count();
        return (activeAccountCount == 0) ? AccountStatus.CREATED : AccountStatus.ACTIVATED;
    }


    @Override
    public CurrentBankAccountDTO saveCurrentBankAccount(double initialBalance, double overDraft, Long customerId)
            throws CustomerNotFoundException {
        log.info("Saving new Current Account for customer ID: {}", customerId);
        Customer customer = customerRepository.findById(customerId).orElseThrow(
                () -> new CustomerNotFoundException("Customer not found with ID: " + customerId)
        );
        CurrentAccount currentAccount = new CurrentAccount();
        currentAccount.setId(UUID.randomUUID().toString());
        currentAccount.setCreatedAt(new Date());
        currentAccount.setBalance(initialBalance);
        currentAccount.setOverDraft(overDraft);
        currentAccount.setCustomer(customer);
        currentAccount.setStatus(determineInitialAccountStatus(customer));

        CurrentAccount savedBankAccount = bankAccountRepository.save(currentAccount);
        return dtoMapper.fromCurrentBankAccount(savedBankAccount);
    }

    @Override
    public SavingBankAccountDTO saveSavingBankAccount(double initialBalance, double interestRate, Long customerId)
            throws CustomerNotFoundException {
        log.info("Saving new Saving Account for customer ID: {}", customerId);
        Customer customer = customerRepository.findById(customerId).orElseThrow(
                () -> new CustomerNotFoundException("Customer not found with ID: " + customerId)
        );
        SavingAccount savingAccount = new SavingAccount();
        savingAccount.setId(UUID.randomUUID().toString());
        savingAccount.setCreatedAt(new Date());
        savingAccount.setBalance(initialBalance);
        savingAccount.setInterestRate(interestRate);
        savingAccount.setCustomer(customer);
        savingAccount.setStatus(determineInitialAccountStatus(customer));

        SavingAccount savedBankAccount = bankAccountRepository.save(savingAccount);
        return dtoMapper.fromSavingBankAccount(savedBankAccount);
    }

    @Override
    public List<CustomerDTO> listCustomers() {
        log.info("Listing all customers");
        List<Customer> customers = customerRepository.findAll();
        return customers.stream()
                .map(dtoMapper::fromCustomer)
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomerDTO> searchCustomers(String keyword) {
        log.info("Searching customers with keyword: '{}'", keyword);
        if (keyword == null || keyword.trim().isEmpty()) {
            return listCustomers();
        }
        List<Customer> customers = customerRepository.searchCustomer(keyword.trim());
        return customers.stream()
                .map(dtoMapper::fromCustomer)
                .collect(Collectors.toList());
    }

    @Override
    public BankAccountDTO getBankAccount(String accountId) throws BankAccountNotFoundException {
        log.info("Fetching bank account ID: {}", accountId);
        BankAccount bankAccount = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new BankAccountNotFoundException("BankAccount not found with ID: " + accountId));


        if (bankAccount instanceof SavingAccount savingAccount) {
            return dtoMapper.fromSavingBankAccount(savingAccount);
        } else if (bankAccount instanceof CurrentAccount currentAccount) {
            return dtoMapper.fromCurrentBankAccount(currentAccount);
        } else {

            log.error("Unknown BankAccount subtype encountered for ID: {}", accountId);
            throw new BankAccountNotFoundException("Unknown BankAccount type for ID: " + accountId);
        }
    }    @Override
    public BankAccountDTO changeAccountStatus(String accountId, AccountStatus status) throws BankAccountNotFoundException {
        log.info("Changing account status for ID: {} to {}", accountId, status);
        
        BankAccount bankAccount = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new BankAccountNotFoundException("BankAccount not found with ID: " + accountId));
        
        AccountStatus oldStatus = bankAccount.getStatus();
        
        validateStatusTransition(oldStatus, status);
        
        bankAccount.setStatus(status);
        bankAccountRepository.save(bankAccount);
        
        log.info("Account {} status changed from {} to {}", accountId, oldStatus, status);
        
        // Return the updated account DTO
        if (bankAccount instanceof SavingAccount savingAccount) {
            return dtoMapper.fromSavingBankAccount(savingAccount);
        } else if (bankAccount instanceof CurrentAccount currentAccount) {
            return dtoMapper.fromCurrentBankAccount(currentAccount);
        } else {
            log.error("Unknown BankAccount subtype encountered for ID: {}", accountId);
            throw new BankAccountNotFoundException("Unknown BankAccount type for ID: " + accountId);
        }
    }

    private void checkAccountStatus(BankAccount bankAccount)
            throws BankAccountActionNotAllowedException {
        if (bankAccount.getStatus() == AccountStatus.SUSPENDED || bankAccount.getStatus() == AccountStatus.CLOSED) {
            throw new BankAccountActionNotAllowedException(
                    "Action not allowed on account with status: " + bankAccount.getStatus()
            );
        }


    }


    @Override
    public void debit(String accountId, double amount, String description)
            throws BankAccountNotFoundException, BalanceNotSufficientException, BankAccountActionNotAllowedException {
        log.info("Debiting account {}: Amount={}, Description={}", accountId, amount, description);
        if (amount <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive.");
        }
        BankAccount bankAccount = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new BankAccountNotFoundException("BankAccount not found with ID: " + accountId));

        checkAccountStatus(bankAccount);

        if (bankAccount.getBalance() < amount) {
            throw new BalanceNotSufficientException("Balance not sufficient for debit of " + amount);
        }

        AccountOperation accountOperation = new AccountOperation();
        accountOperation.setType(OperationType.DEBIT);
        accountOperation.setAmount(amount);
        accountOperation.setDescription(description != null ? description : "Debit Operation");
        accountOperation.setOperationDate(new Date());
        accountOperation.setBankAccount(bankAccount);
        accountOperationRepository.save(accountOperation);

        bankAccount.setBalance(bankAccount.getBalance() - amount);
        bankAccountRepository.save(bankAccount);
    }

    @Override
    public void credit(String accountId, double amount, String description) throws BankAccountNotFoundException, BankAccountActionNotAllowedException {
        log.info("Crediting account {}: Amount={}, Description={}", accountId, amount, description);
        if (amount <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive.");
        }
        BankAccount bankAccount = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new BankAccountNotFoundException("BankAccount not found with ID: " + accountId));

        checkAccountStatus(bankAccount);

        AccountOperation accountOperation = new AccountOperation();
        accountOperation.setType(OperationType.CREDIT);
        accountOperation.setAmount(amount);
        accountOperation.setDescription(description != null ? description : "Credit Operation");
        accountOperation.setOperationDate(new Date());
        accountOperation.setBankAccount(bankAccount);
        accountOperationRepository.save(accountOperation);

        bankAccount.setBalance(bankAccount.getBalance() + amount);
        bankAccountRepository.save(bankAccount);
    }

    @Override
    public void transfer(String accountIdSource, String accountIdDestination, double amount)
            throws BankAccountNotFoundException, BalanceNotSufficientException, BankAccountActionNotAllowedException {
        log.info("Transferring {} from {} to {}", amount, accountIdSource, accountIdDestination);
        if (amount <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive.");
        }
        if (accountIdSource.equals(accountIdDestination)) {
            throw new IllegalArgumentException("Source and destination accounts cannot be the same.");
        }


        debit(accountIdSource, amount, "Transfer to " + accountIdDestination);
        credit(accountIdDestination, amount, "Transfer from " + accountIdSource);
    }

    @Override
    public List<BankAccountDTO> bankAccountList() {
        return bankAccountList(false);
    }

    @Override
    public List<BankAccountDTO> bankAccountList(boolean includeInactive) {
        log.info("Listing {} bank accounts", includeInactive ? "ALL (including inactive/closed)" : "ACTIVE only");

        List<BankAccount> bankAccounts = bankAccountRepository.findAll();

        return bankAccounts.stream()
                .filter(account -> includeInactive ||
                        (account.getStatus() != AccountStatus.SUSPENDED && account.getStatus() != AccountStatus.CLOSED))
                .map(bankAccount -> {
                    if (bankAccount instanceof SavingAccount sa) {
                        return dtoMapper.fromSavingBankAccount(sa);
                    } else if (bankAccount instanceof CurrentAccount ca) {
                        return dtoMapper.fromCurrentBankAccount(ca);
                    }

                    log.warn("Encountered an unexpected BankAccount subtype during list mapping: {}", bankAccount.getClass().getName());
                    return null;
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public CustomerDTO getCustomer(Long customerId) throws CustomerNotFoundException {
        log.info("Fetching customer ID: {}", customerId);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + customerId));
        return dtoMapper.fromCustomer(customer);
    }

    @Override
    public List<AccountOperationDTO> accountHistory(String accountId) throws BankAccountNotFoundException {
        log.info("Fetching all operation history for account ID: {}", accountId);

        if (!bankAccountRepository.existsById(accountId)) {
            throw new BankAccountNotFoundException("BankAccount not found with ID: " + accountId + " for history lookup.");
        }
        List<AccountOperation> accountOperations = accountOperationRepository.findByBankAccountId(accountId);


        return accountOperations.stream()
                .map(dtoMapper::fromAccountOperation)
                .collect(Collectors.toList());
    }

    @Override
    public AccountHistoryDTO getAccountHistory(String accountId, int page, int size) throws BankAccountNotFoundException {
        log.info("Fetching paginated history for account ID: {}, Page: {}, Size: {}", accountId, page, size);
        BankAccount bankAccount = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new BankAccountNotFoundException("BankAccount not found with ID: " + accountId));


        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "operationDate"));
        Page<AccountOperation> accountOperationsPage = accountOperationRepository.findByBankAccountId(accountId, pageRequest);

        List<AccountOperationDTO> accountOperationDTOS = accountOperationsPage.getContent().stream()
                .map(dtoMapper::fromAccountOperation)
                .collect(Collectors.toList());

        AccountHistoryDTO accountHistoryDTO = new AccountHistoryDTO();
        accountHistoryDTO.setAccountOperationDTOS(accountOperationDTOS);
        accountHistoryDTO.setAccountId(bankAccount.getId());
        accountHistoryDTO.setBalance(bankAccount.getBalance());
        accountHistoryDTO.setCurrentPage(accountOperationsPage.getNumber());
        accountHistoryDTO.setPageSize(accountOperationsPage.getSize());
        accountHistoryDTO.setTotalPages(accountOperationsPage.getTotalPages());

        return accountHistoryDTO;
    }

    @Override
    public List<BankAccountDTO> getAccountsByCustomerId(Long customerId) throws CustomerNotFoundException {
        log.info("Fetching bank accounts for customer ID: {}", customerId);
        
        // Check if customer exists
        if (!customerRepository.existsById(customerId)) {
            throw new CustomerNotFoundException("Customer not found with ID: " + customerId);
        }
        
        // Get accounts for this customer
        List<BankAccount> accounts = bankAccountRepository.findByCustomerId(customerId);
        
        // Convert to DTOs and return
        return accounts.stream()
            .map(bankAccount -> {
                if (bankAccount instanceof SavingAccount sa) {
                    return dtoMapper.fromSavingBankAccount(sa);
                } else if (bankAccount instanceof CurrentAccount ca) {
                    return dtoMapper.fromCurrentBankAccount(ca);
                }
                
                log.warn("Encountered an unexpected BankAccount subtype: {}", bankAccount.getClass().getName());
                return null;
            })
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toList());
    }

    private void validateStatusTransition(AccountStatus fromStatus, AccountStatus toStatus) {
        // Same status - no change needed
        if (fromStatus == toStatus) {
            throw new IllegalArgumentException("Account is already in " + toStatus + " status");
        }
        
        boolean isValidTransition = switch (fromStatus) {
            case CREATED -> {
                // From CREATED: Can only activate or close (if never activated)
                yield toStatus == AccountStatus.ACTIVATED || toStatus == AccountStatus.CLOSED;
            }
            case ACTIVATED -> {
                // From ACTIVATED: Can suspend or close
                yield toStatus == AccountStatus.SUSPENDED || toStatus == AccountStatus.CLOSED;
            }
            case SUSPENDED -> {
                // From SUSPENDED: Can reactivate or close
                yield toStatus == AccountStatus.ACTIVATED || toStatus == AccountStatus.CLOSED;
            }
            case CLOSED -> {
                // From CLOSED: No transitions allowed (permanent state)
                yield false;
            }
        };
        
        if (!isValidTransition) {
            throw new IllegalArgumentException(
                String.format("Invalid status transition: Cannot change from %s to %s", fromStatus, toStatus)
            );
        }
    }
}