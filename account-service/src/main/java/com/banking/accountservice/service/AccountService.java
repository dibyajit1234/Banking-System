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
}
