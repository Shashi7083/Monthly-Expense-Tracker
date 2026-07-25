package com.shashi.sms.mapper;

import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;

import com.shashi.sms.model.SMSMessage;

public class SMSMessageMapper implements FieldSetMapper<SMSMessage> {

    @Override
    public SMSMessage mapFieldSet(FieldSet fieldSet) {
        SMSMessage smsMessage = new SMSMessage();

        try{
            smsMessage.setSender(fieldSet.readString("Sender"));
            smsMessage.setDate(fieldSet.readString("Date"));
            smsMessage.setRead(fieldSet.readBoolean("Read"));
            smsMessage.setType(fieldSet.readString("Type"));
            smsMessage.setThread(fieldSet.readString("Thread"));
            smsMessage.setService(fieldSet.readString("Service"));
            smsMessage.setMessage(fieldSet.readString("Message"));
        } catch (Exception e) {
            System.out.println("Exception in parsing : "+e.getMessage());
            return null;
        }

        return smsMessage;
    }
    
}
