package com.shashi.sms.serviceImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class ExcelAllMessagesServiceImpl {
    
    private Workbook workbook;
    private File file;

    public synchronized void openWorkbook(String fileName) throws IOException {
        if (this.workbook != null) {
            return; // Already open
        }
        this.file = new File(fileName);

        if (this.file.exists()) {
            try (FileInputStream fileIn = new FileInputStream(this.file)) {
                this.workbook = new XSSFWorkbook(fileIn);
            }
            System.out.println("Opened existing workbook: " + fileName);
        } else {
            this.workbook = new XSSFWorkbook();
            Sheet sheet = this.workbook.createSheet("Transactions");
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Message");
            headerRow.createCell(1).setCellValue("Transaction");
            System.out.println("Created new workbook with headers: " + fileName);
        }
    }

    public synchronized void closeWorkbook() throws IOException {
        if (this.workbook == null) {
            return; // Already closed safely
        }
        String fileName = this.file != null ? this.file.getName() : "workbook";
        try {
            if (this.file != null) {
                try (FileOutputStream fileOut = new FileOutputStream(this.file)) {
                    this.workbook.write(fileOut);
                }
            }
        } finally {
            this.workbook.close();
            this.workbook = null;
            this.file = null;
        }
        System.out.println("Successfully saved and closed " + fileName);
    }

    public synchronized void appendSingleTransaction(String message, Boolean isTransaction) {
        if (this.workbook == null) {
            return; // Skip appending if workbook is not open
        }
        Sheet sheet = this.workbook.getSheet("Transactions");
        if (sheet == null) {
             sheet = this.workbook.createSheet("Transactions");
             Row headerRow = sheet.createRow(0);
             headerRow.createCell(0).setCellValue("Message");
             headerRow.createCell(1).setCellValue("Transaction");
        }

        int rowNum = sheet.getLastRowNum() + 1;
        Row row = sheet.createRow(rowNum);

        Cell messageCell = row.createCell(0);
        messageCell.setCellValue(message);

        Cell transactionCell = row.createCell(1);
        transactionCell.setCellValue(isTransaction);
        System.out.println("Appended transaction: " + message);
    }
}
