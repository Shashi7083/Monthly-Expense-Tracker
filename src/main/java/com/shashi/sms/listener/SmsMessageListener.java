package com.shashi.sms.listener;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;

import com.shashi.sms.serviceImpl.ExcelAllMessagesServiceImpl;
import com.shashi.sms.serviceImpl.TransactionOutputServiceImpl;

public class SmsMessageListener implements JobExecutionListener {

    @Autowired
    private TransactionOutputServiceImpl transactionOutputService;

    @Autowired
    private ExcelAllMessagesServiceImpl excelAllMessagesService;

    @Override
    public void afterJob(JobExecution jobExecution) {
        try {
            transactionOutputService.closeOutputFile();
            System.out.println("closing file successfully");
        } catch (Exception e) {
            System.out.println("Error in closing output file: " + e.getMessage());
        }

        try {
            excelAllMessagesService.closeWorkbook();
            System.out.println("Excel workbook closed successfully");
        } catch (Exception e) {
            System.out.println("Error in closing Excel workbook: " + e.getMessage());
        }
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        try {
             transactionOutputService.openOutputFile();
            System.out.println("opening file successfully");
           
        } catch (Exception e) {
            System.err.println("Error in opening output file: " + e.getMessage());
        }

        try {
            excelAllMessagesService.openWorkbook("all_messages.xlsx");
            System.out.println("Excel workbook opened successfully");
        } catch (Exception e) {
            System.err.println("Error in opening Excel workbook: " + e.getMessage());
        }
    }
    
}
