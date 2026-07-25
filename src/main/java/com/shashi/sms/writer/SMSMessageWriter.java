package com.shashi.sms.writer;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

import com.shashi.sms.model.BankTransaction;
import com.shashi.sms.repository.BankTransactionRepository;

public class SMSMessageWriter implements ItemWriter<BankTransaction> {

    @Autowired
    private BankTransactionRepository bankTransactionRepository;

    @Override
    public void write(Chunk<? extends BankTransaction> chunk) throws Exception {
        bankTransactionRepository.saveAll(chunk.getItems());
        System.out.println("Saved " + chunk.size() + " transactions to MongoDB.");
    }

}
