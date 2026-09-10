package com.banking.transactionservice.entity;

/*
* pending-> processing-> completed
*                      -> pending_verification(suspicious detected)
*                               -> completed
*                               -> flagged(saga refund)
*                       -> failed
*                       -> flagged
* */
public enum TransactionStatus {
    PENDING,
    PROCESSING,
    COMPLETED
}
