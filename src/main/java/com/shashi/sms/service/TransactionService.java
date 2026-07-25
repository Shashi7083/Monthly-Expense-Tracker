package com.shashi.sms.service;

import com.shashi.sms.model.BankTransaction;
import com.shashi.sms.model.SMSMessage;

public interface TransactionService {

    public BankTransaction transaction(SMSMessage smsMessage) throws Exception;

    public boolean isBankTransaction(SMSMessage smsMessage) throws Exception;

    public String detectBankName(String sender, String message);
    
}
