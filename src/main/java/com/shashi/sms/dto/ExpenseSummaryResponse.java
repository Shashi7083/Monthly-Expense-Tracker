package com.shashi.sms.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.shashi.sms.model.BankTransaction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExpenseSummaryResponse {

    private double totalDebit;
    private double totalCredit;
    private int totalMessagesCount;
    private int totalTransactionsCount;
    private int totalDebitCount;
    private int totalCreditCount;
    private Map<String, Double> bankWiseExpenses;
    private Map<String, Double> bankWiseCredits;
    private Map<String, MonthlySummary> monthlyExpenses;

    @JsonIgnore
    private List<BankTransaction> transactions;

}
