package com.banking.transactionservice.service;

import com.banking.transactionservice.client.AccountServiceClient;
import com.banking.transactionservice.dto.TransactionResponse;
import com.banking.transactionservice.dto.TransferRequest;
import com.banking.transactionservice.entity.Transaction;
import com.banking.transactionservice.event.TransactionInitiatedEvent;
import com.banking.transactionservice.repository.TransactionRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {
    
    private final TransactionRepository transactionRepository;
    private final AccountServiceClient accountServiceClient;
    private final KafkaTemplate<String,Object> kafkaTemplate;

    private static final String TRANSACTION_INITIATED_TOPIC = "transaction.initiated";
    private static final String TRANSACTION_COMPLETED_TOPIC = "transaction.completed";
    private static final String TRANSACTION_REFUNDED_TOPIC = "transaction.refunded";

    /*
    * SAGA step 1 - Initiate Transfer
    * deducts from sender via faint
    * saves transaction as processing
    * publish events to kafka for fraud check
    * returns
    * */
    public TransactionResponse transfer(@Valid TransferRequest request) {
        log.info("SAGA start- transfer: {} -> {} amount {}",
                request.getSenderAccount(),
                request.getSenderAccount(),
                request.getAmount());

        accountServiceClient.deductBalance(request.getSenderAccount(),request.getAmount());
        Transaction transaction = new Transaction();
        transaction.setSenderAccount(request.getSenderAccount());
        transaction.setReceiverAccount(request.getReceiverAccount());
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Transaction saved as processing TransactionId: {}",savedTransaction.getId());

        //SAGA step 2 : publish for fraud detection
        TransactionInitiatedEvent event = new TransactionInitiatedEvent();
        event.setTransactionId(savedTransaction.getId());
        event.setAmount(savedTransaction.getAmount());
        event.setDescription(savedTransaction.getDescription());
        event.setSenderAccountNumber(savedTransaction.getSenderAccount());
        event.setReceiverAccountNumber(savedTransaction.getReceiverAccount());

        kafkaTemplate.send(TRANSACTION_INITIATED_TOPIC,savedTransaction.getId(),event);
        log.info("saga step :2 - Transaction Event published {}",savedTransaction.getId());

        return mapToResponse(savedTransaction);
    }
    private TransactionResponse mapToResponse(Transaction transaction){
        TransactionResponse response = new TransactionResponse();
        response.setId(transaction.getId());
        response.setAmount(transaction.getAmount());
        response.setDescription(transaction.getDescription());
        response.setReceiverAccount(transaction.getReceiverAccount());
        response.setType(transaction.getType());
        response.setStatus(transaction.getStatus());
        response.setDescription(transaction.getDescription());
        response.setReferenceNumber(transaction.getReferenceNumber());
        response.setCreatedAt(transaction.getCreatedAt());
        response.setCompletedAt(transaction.getCompletedAt());
        response.setFailureReason(transaction.getFailureReason());
        return response;
    }

    public TransactionResponse getTransaction(String transactionId) {
        return mapToResponse(transactionRepository.findById(transactionId).
                orElseThrow(()->new RuntimeException("Transactoin not found"))
                );
    }

    public List<TransactionResponse> getTransactionHistory(String accountNumber) {
            return transactionRepository.findBySenderAccountNumberOrderByCreatedAtDesc(accountNumber)
                    .stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
    }
}
