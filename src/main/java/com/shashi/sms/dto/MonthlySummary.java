package com.shashi.sms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySummary {
    private double debited;
    private double credited;
    private double totalAmount;
    private int transactionCount;
}
