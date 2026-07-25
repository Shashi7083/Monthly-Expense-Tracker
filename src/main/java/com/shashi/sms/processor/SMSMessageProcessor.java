package com.shashi.sms.processor;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

import com.shashi.sms.model.BankTransaction;
import com.shashi.sms.model.SMSMessage;
import com.shashi.sms.service.TransactionService;
import com.shashi.sms.serviceImpl.ExcelAllMessagesServiceImpl;
import com.shashi.sms.serviceImpl.TransactionOutputServiceImpl;

public class SMSMessageProcessor implements ItemProcessor<SMSMessage, BankTransaction> {

    @Autowired
    TransactionService transactionService;
    
    @Autowired
    TransactionOutputServiceImpl transactionOutputService;

    @Autowired
    ExcelAllMessagesServiceImpl excelAllMessagesService;

    @Override
    public BankTransaction process(SMSMessage item) throws Exception {

        

        String line = item.getMessage();
        try{
            transactionOutputService.writeLine(line); // Write the raw message to output file
        } catch (Exception e) {
            System.out.println("Exception in writing message: "+e.getMessage());
        }

        if(!transactionService.isBankTransaction(item)) {
            excelAllMessagesService.appendSingleTransaction(item.getMessage(), false);
            return null; // Skip non-transaction messages
        }else{
            excelAllMessagesService.appendSingleTransaction(item.getMessage(), true);
        }

        BankTransaction bankTransaction = transactionService.transaction(item);

        if(bankTransaction == null) {
            return null; // Skip if transaction could not be created
        }

        return bankTransaction;
    }
    
}
