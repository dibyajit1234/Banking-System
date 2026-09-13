package com.banking.frauddection.service;

import com.banking.frauddection.client.AccountServiceClient;
import com.banking.frauddection.model.FraudCheckResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class FraudDetectionService {

    private final KafkaTemplate<String,Object> kafkaTemplate;
    private final AccountServiceClient accountServiceClient;

    private static final String VERIFICATION_REQUIRED_TOPIC = "verification.required";
    private static final String FRAUD_CHECK_CLEAN_RESULT = "fraud.check.clean";

    public void checkTransaction(Map<String, Object> payload) {
        String transactionId = (String) payload.get("transactionId");
        String accountNumber = (String)payload.get("accountNumber");
        String amount = (String)payload.get("amount");

        //Fetch real balance from account service
        BigDecimal senderBalance = accountServiceClient.getBalance(accountNumber);

        log.info("Checking transaction:{}, account number: {}, amount : {}, balance : {},",
                transactionId,accountNumber,amount,senderBalance);
        FraudCheckResult result = performFraudCheck(accountNumber,amount,senderBalance);

        if(result.isFraud()){
            log.info("Suspicious activity detected, account: {}, reason :{}, requesting OTP verification"
                    ,accountNumber,result.getReason());
            Map<String,Object> verificationEvent = new HashMap<>();
            verificationEvent.put("transactionId",transactionId);
            verificationEvent.put("amount",amount);
            verificationEvent.put("accountNumber",accountNumber);
            verificationEvent.put("reason",result.getReason());

            kafkaTemplate.send(VERIFICATION_REQUIRED_TOPIC,transactionId,verificationEvent);
        }else{
            //Transaction is clean
            log.info("My Transaction is clean");
            Map<String,Object> transactionCleanEvent = new HashMap<>();
            transactionCleanEvent.put("transactionId",transactionId);
            transactionCleanEvent.put("isFraud",false);
            transactionCleanEvent.put("reason",null);

            kafkaTemplate.send(FRAUD_CHECK_CLEAN_RESULT,transactionId,transactionCleanEvent);
        }

    }

    private FraudCheckResult performFraudCheck(
            String accountNumber
            , BigDecimal amount
            , BigDecimal senderBalance) {

        //pattern 1: velocity check
        if(isVelocityExceeded)return new FraudCheckResult(true,"To many transactions in 60 seconds - velocity limit exceeded");

        //pattern 2 : amount check
        if(isAccountSuspicious(accountNumber,amount)) return new FraudCheckResult(true,"Unusual transaction amount - exceeds yous average")

        //pattern 3: Balance check
        if(senderBalance.compareTo(BigDecimal.ZERO)>0 && isBalanceCheckFailed(senderBalance,amount))
            return new FraudCheckResult(true,"Transaction exceed 90 % of account balance");

        return new FraudCheckResult(false,null);

    }
}
