package com.banking.accountservice.service;

import com.banking.accountservice.dto.AccountResponse;
import com.banking.accountservice.dto.CreateAccountRequest;
import com.banking.accountservice.entity.Account;
import com.banking.accountservice.entity.AccountStatus;
import com.banking.accountservice.entity.AccountType;
import com.banking.accountservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;

    public AccountResponse createAccount(CreateAccountRequest request){
        log.info("creating the account for {}:"+ request.getEmail());
        if(accountRepository.existsByEmail(request.getEmail()))
            throw new RuntimeException("Account already exists for email :"+request.getEmail());
        Account account = new Account();
        account.setAccountHolderName(request.getAccountHolderName());
        account.setEmail(request.getEmail());
        account.setAccountType(request.getAccountType());
        account.setBalance(request.getInitialDeposit());
        account.setPhone(request.getPhone());
        account.setStatus(AccountStatus.ACTIVE);
        account.setAccountNumber(generateAccountNumber());
        account.setDailyTransactionLimit(
                request.getAccountType()== AccountType.SAVINGS
                ?new BigDecimal("100000")
                        :new BigDecimal("500000")
        );
        Account savedAccount = accountRepository.save(account);
        log.info("Account created account number :"+savedAccount.getAccountNumber());
        return mapToResponse(savedAccount);
    }
//generate account number
private String generateAccountNumber() {
    String accountNumber;
    SecureRandom secureRandom = new SecureRandom();

    do {
        long number = secureRandom.nextLong(1_000_000_000_000L);
        accountNumber = String.format("%12d", number);

    } while (accountRepository.existsByAccountNumber(accountNumber));

    return accountNumber;
}

    private AccountResponse mapToResponse(Account account){
        AccountResponse  response = new AccountResponse();
        response.setId(account.getId());
        response.setAccountHolderName(account.getAccountHolderName());
        response.setEmail(account.getEmail());
        response.setAccountNumber(account.getAccountNumber());
        response.setAccountType(account.getAccountType());
        response.setBalance(account.getBalance());
        response.setPhone(account.getPhone());
        response.setStatus(account.getStatus());
        response.setDailyTransactionLimit(account.getDailyTransactionLimit());
        return response;
    }

    public AccountResponse getAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(()-> new RuntimeException("Account not fount"));
        return mapToResponse(account);
    }

    public BigDecimal getBalance(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(()-> new RuntimeException("Account not fount"));
        return account.getBalance();
    }

    public void blockAccount(String accountNumber) {
        log.info("blocking account : {}",accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(()-> new RuntimeException("Account not fount"));
        account.setStatus(AccountStatus.BLOCKED);
        accountRepository.save(account);
        log.info("blocked accountnumber {}: ",accountNumber);
    }

    public void deductBalance(String accountNumber, BigDecimal amount) {
        log.info("deducting balance {} from account {}",amount,accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(()-> new RuntimeException("Account not fount"));
        if(account.getStatus()!=AccountStatus.ACTIVE)throw new RuntimeException("Account is not active"+accountNumber);
        if(account.getBalance().compareTo(amount)<0) throw new RuntimeException("Insufficient balance for account : "+accountNumber);
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);
        log.info("Balance updated new balance : {}",account.getBalance());
    }

    public void creditBalance(String accountNumber, BigDecimal amount) {
        log.info("Crediting balance {} to account{}",amount,accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(()-> new RuntimeException("Account not fount"));
        if(account.getStatus()!=AccountStatus.ACTIVE)throw new RuntimeException("Account is not active"+accountNumber);
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
        log.info("Balance updated, new Balance : {}",account.getBalance());
    }
}
