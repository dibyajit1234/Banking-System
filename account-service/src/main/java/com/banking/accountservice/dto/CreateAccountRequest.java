package com.banking.accountservice.dto;

import com.banking.accountservice.entity.AccountType;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAccountRequest {

    @NotBlank(message = "account holder name is required")
    private String accountHolderName;

    @NotBlank(message = "Account number should not be blank")
    private String accountNumber;

    @NotBlank(message = "email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone is required")
    private  String phone;

    @NotBlank(message = "Account type is required")
    private AccountType accountType;

    @NotBlank(message = "Initial Deposit is required")
    @Positive(message = "Initial deposit must be positive")
    private BigDecimal initialDeposit;

}
