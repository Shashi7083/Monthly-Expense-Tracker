package com.shashi.sms.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.shashi.sms.model.BankTransaction;

@Repository
public interface BankTransactionRepository extends MongoRepository<BankTransaction, String>{

    
} 