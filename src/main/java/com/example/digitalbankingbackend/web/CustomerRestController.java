package com.example.digitalbankingbackend.web;

import com.example.digitalbankingbackend.dtos.CustomerDTO;
import com.example.digitalbankingbackend.dtos.BankAccountDTO;
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
@RequestMapping("/api/v1/customers")
@AllArgsConstructor
@Slf4j
@CrossOrigin("*")
public class CustomerRestController {

    private final BankAccountService bankAccountService;

    @GetMapping
    public ResponseEntity<List<CustomerDTO>> getAllCustomers() {
        log.info("REST request to get all customers");
        List<CustomerDTO> customers = bankAccountService.listCustomers();
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerDTO> getCustomerById(@PathVariable Long id) throws CustomerNotFoundException {
        log.info("REST request to get customer by ID: {}", id);
        CustomerDTO customer = bankAccountService.getCustomer(id);
        return ResponseEntity.ok(customer);
    }

    @GetMapping("/search")
    public ResponseEntity<List<CustomerDTO>> searchCustomers(@RequestParam(name = "keyword", defaultValue = "") String keyword) {
        log.info("REST request to search customers with keyword: '{}'", keyword);
        List<CustomerDTO> customers = bankAccountService.searchCustomers(keyword);
        return ResponseEntity.ok(customers);
    }

    @PostMapping
    public ResponseEntity<CustomerDTO> createCustomer(@Valid @RequestBody CustomerDTO customerDTO) {
        log.info("REST request to create new customer: {}", customerDTO.getName());
        CustomerDTO savedCustomer = bankAccountService.saveCustomer(customerDTO);
        return new ResponseEntity<>(savedCustomer, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerDTO> updateCustomer(@PathVariable Long id, @Valid @RequestBody CustomerDTO customerDTO) throws CustomerNotFoundException {
        log.info("REST request to update customer with ID: {}", id);
        customerDTO.setId(id); 
        CustomerDTO updatedCustomer = bankAccountService.updateCustomer(customerDTO);
        return ResponseEntity.ok(updatedCustomer);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) throws CustomerNotFoundException {
        log.info("REST request to delete customer with ID: {}", id);
        bankAccountService.deleteCustomer(id);
        return ResponseEntity.noContent().build(); 
    }

    @GetMapping("/{customerId}/accounts")
    public ResponseEntity<List<BankAccountDTO>> getCustomerAccounts(@PathVariable Long customerId) throws CustomerNotFoundException {
        log.info("REST request to get accounts for customer ID: {}", customerId);
        List<BankAccountDTO> accounts = bankAccountService.getAccountsByCustomerId(customerId);
        return ResponseEntity.ok(accounts);
    }
}