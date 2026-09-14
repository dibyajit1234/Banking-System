package com.banking.transactionservice.service;

import com.banking.transactionservice.entity.Transaction;
import com.banking.transactionservice.entity.TransactionStatus;
import com.banking.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionEventConsumer {

    private final TransactionRepository transactionRepository;
    private final RedisTemplate<String,String> redisTemplate;
    private static final long OTP_EXPIRY_MINUTES  =5;

    private final KafkaTemplate<String,Object> kafkaTemplate;

    private static final String TRANSACTION_OTP_GENERATED_TOPIC = "transaction.otp.generated";
    /*
    * Consume verification.required
    * Generate otp and ask to verify
    * */
    public void consumerVerificationRequired(
            @Payload Map<String,Object> payload){
        try {
            String transactionId = (String)payload.get("transactionId");
            String reason = (String) payload.get("reason");
            String accountNumber = (String)payload.get("accountNumber");

            log.info("Verification required - transaction {} reason {}",transactionId,reason);

            Transaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(()-> new RuntimeException("Transaction not fount" + transactionId));
            if (transaction.getStatus()!= TransactionStatus.PROCESSING){
                log.warn("Transaction {} not processing skipping",transactionId);
                return ;
            }
            //Generate 6 digit otp
            String otp = String.format("%06d",(int)(Math.random() * 900000)+100000);

            //store otp in redis - expires in 5 minutes
            String otpKey = "verification:otp"+transactionId;
            redisTemplate.opsForValue().set(otpKey,otp,OTP_EXPIRY_MINUTES, TimeUnit.MINUTES);
            //update status
            transaction.setStatus(TransactionStatus.PENDING_VERIFICATION);
            transactionRepository.save(transaction);
            log.info("otp generated for transaction {} expires in {} minutes"
                    ,transactionId
                    ,OTP_EXPIRY_MINUTES);
            //notify user
            Map<String,Object> otpEvent = new HashMap<>();
            otpEvent.put("transactionId",transaction);
            otpEvent.put("accountNumber",accountNumber);
            otpEvent.put("reason",reason);
            otpEvent.put("otp",otp);
            otpEvent.put("amount",payload.get("amount"));

            kafkaTemplate.send(TRANSACTION_OTP_GENERATED_TOPIC,otpEvent);


        } catch (Exception e) {
            log.info("Error handling verification required {}",e.getMessage());
        }

    }
}
