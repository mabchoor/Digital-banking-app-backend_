package com.example.digitalbankingbackend;

import com.example.digitalbankingbackend.entities.*;
import com.example.digitalbankingbackend.enums.AccountStatus;
import com.example.digitalbankingbackend.enums.OperationType;
import com.example.digitalbankingbackend.repositories.AccountOperationRepository;
import com.example.digitalbankingbackend.repositories.BankAccountRepository;
import com.example.digitalbankingbackend.repositories.CustomerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;


import java.util.Date;
import java.util.UUID;
import java.util.stream.Stream;

@SpringBootApplication
public class DigitalBankingBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(DigitalBankingBackendApplication.class, args);
    }
    
    @Bean
    CommandLineRunner start(CustomerRepository customerRepository,
                            BankAccountRepository bankAccountRepository,
                            AccountOperationRepository accountOperationRepository) {
        return args -> {
            // Create Customers
            Stream.of("Hassan", "Imane", "Mohamed").forEach(name -> {
                Customer customer = new Customer();
                customer.setName(name);
                customer.setEmail(name.toLowerCase() + "@gmail.com");
                customerRepository.save(customer);
            });

            customerRepository.findAll().forEach(customer -> {
                CurrentAccount currentAccount = new CurrentAccount();
                currentAccount.setId(UUID.randomUUID().toString());
                currentAccount.setBalance(Math.random() * 90000 + 10000); // Random balance between 10k and 100k
                currentAccount.setCreatedAt(new Date());
                currentAccount.setStatus(AccountStatus.CREATED);
                currentAccount.setCustomer(customer);
                currentAccount.setOverDraft(5000); // Example overdraft limit
                bankAccountRepository.save(currentAccount);

                // Create a Saving Account
                SavingAccount savingAccount = new SavingAccount();
                savingAccount.setId(UUID.randomUUID().toString());
                savingAccount.setBalance(Math.random() * 120000 + 10000); // Random balance between 10k and 130k
                savingAccount.setCreatedAt(new Date());
                savingAccount.setStatus(AccountStatus.ACTIVATED);
                savingAccount.setCustomer(customer);
                savingAccount.setInterestRate(5.5); // Example interest rate
                bankAccountRepository.save(savingAccount);
            });

            // Create Operations for each account
            bankAccountRepository.findAll().forEach(account -> {
                for (int i = 0; i < 5; i++) { // Create 5 operations per account
                    // Credit Operation
                    AccountOperation creditOp = new AccountOperation();
                    creditOp.setOperationDate(new Date());
                    creditOp.setAmount(Math.random() * 10000 + 1000); // Random amount between 1k and 11k
                    creditOp.setType(OperationType.CREDIT);
                    creditOp.setBankAccount(account);
                    creditOp.setDescription("Credit Operation");
                    accountOperationRepository.save(creditOp);

                    // Debit Operation
                    AccountOperation debitOp = new AccountOperation();
                    debitOp.setOperationDate(new Date());
                    debitOp.setAmount(Math.random() * 5000 + 500); // Random amount between 500 and 5500
                    debitOp.setType(OperationType.DEBIT);
                    debitOp.setBankAccount(account);
                    debitOp.setDescription("Debit Operation");
                    accountOperationRepository.save(debitOp);
                }
            });

            // Fetch and print some data
            System.out.println("--- Customers ---");
            customerRepository.findAll().forEach(System.out::println);

            System.out.println("\n--- Bank Accounts ---");
            bankAccountRepository.findAll().forEach(acc -> {
                System.out.println(acc.getClass().getSimpleName() + ":" + acc.getId() + " Balance:" + acc.getBalance() + " Customer:" + acc.getCustomer().getName());
            });

            System.out.println("\n--- Account Operations (First Account) ---");
            if (!bankAccountRepository.findAll().isEmpty()) {
                String firstAccountId = bankAccountRepository.findAll().get(0).getId();
                accountOperationRepository.findByBankAccountId(firstAccountId).forEach(op -> {
                    System.out.println(op.getOperationDate() + " " + op.getType() + " Amount:" + op.getAmount());
                });
            }
        };
    }
}