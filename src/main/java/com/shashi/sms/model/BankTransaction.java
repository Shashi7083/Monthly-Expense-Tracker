package com.shashi.sms.model;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Document(collection = "bank_transactions")
public class BankTransaction {

    @Id
    private String transactionId;
    
    private String bankName;          
    private String account;           
    private String transactionType;   
    private double amount;            
    private LocalDate date;             
    private LocalTime time;             
    private String reference;        
    private String message;           
    private String source;            
    private String sender;            
         


}
