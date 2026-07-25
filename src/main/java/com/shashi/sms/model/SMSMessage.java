package com.shashi.sms.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SMSMessage {
    
    private String sender;
    private String date;
    private boolean read;
    private String type;
    private String thread;
    private String service;
    private String message;


}
