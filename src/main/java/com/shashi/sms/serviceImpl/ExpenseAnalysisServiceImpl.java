package com.shashi.sms.serviceImpl;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.shashi.sms.dto.ExpenseSummaryResponse;
import com.shashi.sms.model.BankTransaction;
import com.shashi.sms.model.SMSMessage;
import com.shashi.sms.repository.BankTransactionRepository;
import com.shashi.sms.service.ExpenseAnalysisService;
import com.shashi.sms.service.TransactionService;

@Service
public class ExpenseAnalysisServiceImpl implements ExpenseAnalysisService {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private BankTransactionRepository bankTransactionRepository;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("smsMessageJob")
    private Job smsMessageJob;

    @Override
    public ExpenseSummaryResponse analyzeSmsFile(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty or null.");
        }

        // 1. Save uploaded file to local 'uploads' directory
        Path uploadDir = Paths.get("uploads");
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        String fileName = "sms_upload_" + System.currentTimeMillis() + ".txt";
        Path targetPath = uploadDir.resolve(fileName);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        // 2. Launch Spring Batch job to process the saved file
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("inputFile", targetPath.toAbsolutePath().toString())
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(smsMessageJob, jobParameters);

        // 3. Aggregate expense response
        List<BankTransaction> transactions = new ArrayList<>();
        double totalDebit = 0.0;
        double totalCredit = 0.0;
        int totalMessagesCount = 0;
        int totalDebitCount = 0;
        int totalCreditCount = 0;
        Map<String, Double> bankWiseExpenses = new LinkedHashMap<>();
        Map<String, Double> bankWiseCredits = new LinkedHashMap<>();
        Map<String, com.shashi.sms.dto.MonthlySummary> monthlyExpenses = new LinkedHashMap<>();

        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM (MMM)");

        try (BufferedReader reader = Files.newBufferedReader(targetPath, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                if (lineNumber == 1 && line.toLowerCase().contains("sender")
                        && line.toLowerCase().contains("message")) {
                    continue;
                }

                SMSMessage smsMessage = parseSmsLine(line);
                if (smsMessage == null) {
                    continue;
                }

                totalMessagesCount++;

                if (transactionService.isBankTransaction(smsMessage)) {
                    BankTransaction txn = transactionService.transaction(smsMessage);
                    if (txn != null) {
                        transactions.add(txn);

                        double amount = txn.getAmount();
                        String bank = txn.getBankName() != null ? txn.getBankName() : "Unknown Bank";

                        String monthKey = "Unknown Month";
                        if (txn.getDate() != null) {
                            monthKey = txn.getDate().format(monthFormatter);
                        }

                        com.shashi.sms.dto.MonthlySummary monthSummary = monthlyExpenses.computeIfAbsent(monthKey,
                                k -> new com.shashi.sms.dto.MonthlySummary(0.0, 0.0, 0.0, 0));
                        monthSummary.setTransactionCount(monthSummary.getTransactionCount() + 1);

                        if ("Debit".equalsIgnoreCase(txn.getTransactionType())) {
                            totalDebit += amount;
                            totalDebitCount++;
                            bankWiseExpenses.put(bank, Math.round((bankWiseExpenses.getOrDefault(bank, 0.0) + amount) * 100.0) / 100.0);
                            monthSummary.setDebited(Math.round((monthSummary.getDebited() + amount) * 100.0) / 100.0);
                        } else if ("Credit".equalsIgnoreCase(txn.getTransactionType())) {
                            totalCredit += amount;
                            totalCreditCount++;
                            bankWiseCredits.put(bank, Math.round((bankWiseCredits.getOrDefault(bank, 0.0) + amount) * 100.0) / 100.0);
                            monthSummary.setCredited(Math.round((monthSummary.getCredited() + amount) * 100.0) / 100.0);
                        }
                        monthSummary.setTotalAmount(Math.round((monthSummary.getDebited() + monthSummary.getCredited()) * 100.0) / 100.0);
                    }
                }
            }
        }

        ExpenseSummaryResponse response = new ExpenseSummaryResponse();
        response.setTotalDebit(Math.round(totalDebit * 100.0) / 100.0);
        response.setTotalCredit(Math.round(totalCredit * 100.0) / 100.0);
        response.setTotalMessagesCount(totalMessagesCount);
        response.setTotalTransactionsCount(transactions.size());
        response.setTotalDebitCount(totalDebitCount);
        response.setTotalCreditCount(totalCreditCount);
        response.setBankWiseExpenses(bankWiseExpenses);
        response.setBankWiseCredits(bankWiseCredits);
        response.setMonthlyExpenses(monthlyExpenses);
        response.setTransactions(transactions);

        return response;
    }

    @Override
    public byte[] exportExpenseExcel(MultipartFile file) throws Exception {
        ExpenseSummaryResponse summary = analyzeSmsFile(file);

        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Define styles
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);

            // Sheet 1: Summary Overview
            Sheet summarySheet = workbook.createSheet("Expense Summary");
            summarySheet.setColumnWidth(0, 8000);
            summarySheet.setColumnWidth(1, 6000);
            summarySheet.setColumnWidth(2, 6000);
            summarySheet.setColumnWidth(3, 6000);
            summarySheet.setColumnWidth(4, 4000);

            int rowIdx = 0;

            // Title
            Row titleRow = summarySheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Bank SMS Expense Analysis Report");
            titleCell.setCellStyle(titleStyle);

            rowIdx++; // Blank row

            // Overall Summary Card Table
            Row cardHeaderRow = summarySheet.createRow(rowIdx++);
            createCell(cardHeaderRow, 0, "Metric", headerStyle);
            createCell(cardHeaderRow, 1, "Amount (INR)", headerStyle);

            Row r1 = summarySheet.createRow(rowIdx++);
            createCell(r1, 0, "Total Transactions", dataStyle);
            createCell(r1, 1, (double) summary.getTotalTransactionsCount(), dataStyle);

            Row r2 = summarySheet.createRow(rowIdx++);
            createCell(r2, 0, "Total Debited (Expenses)", dataStyle);
            createCell(r2, 1, summary.getTotalDebit(), currencyStyle);

            Row r3 = summarySheet.createRow(rowIdx++);
            createCell(r3, 0, "Total Credited (Income/Refunds)", dataStyle);
            createCell(r3, 1, summary.getTotalCredit(), currencyStyle);

            Row r4 = summarySheet.createRow(rowIdx++);
            createCell(r4, 0, "Net Balance Change", dataStyle);
            createCell(r4, 1, summary.getTotalCredit() - summary.getTotalDebit(), currencyStyle);

            rowIdx += 2; // Blank rows

            // Bank-wise Deducted Breakdown Table
            Row bankHeaderRow = summarySheet.createRow(rowIdx++);
            createCell(bankHeaderRow, 0, "Bank Name (Deducted / Debited)", headerStyle);
            createCell(bankHeaderRow, 1, "Total Deducted (INR)", headerStyle);

            if (summary.getBankWiseExpenses() != null) {
                for (Map.Entry<String, Double> entry : summary.getBankWiseExpenses().entrySet()) {
                    Row row = summarySheet.createRow(rowIdx++);
                    createCell(row, 0, entry.getKey(), dataStyle);
                    createCell(row, 1, entry.getValue(), currencyStyle);
                }
            }

            rowIdx += 2; // Blank rows

            // Bank-wise Received Breakdown Table
            Row bankCreditHeaderRow = summarySheet.createRow(rowIdx++);
            createCell(bankCreditHeaderRow, 0, "Bank Name (Received / Credited)", headerStyle);
            createCell(bankCreditHeaderRow, 1, "Total Received (INR)", headerStyle);

            if (summary.getBankWiseCredits() != null) {
                for (Map.Entry<String, Double> entry : summary.getBankWiseCredits().entrySet()) {
                    Row row = summarySheet.createRow(rowIdx++);
                    createCell(row, 0, entry.getKey(), dataStyle);
                    createCell(row, 1, entry.getValue(), currencyStyle);
                }
            }

            rowIdx += 2; // Blank rows

            // Monthly Breakdown Table
            Row monthHeaderRow = summarySheet.createRow(rowIdx++);
            createCell(monthHeaderRow, 0, "Month", headerStyle);
            createCell(monthHeaderRow, 1, "Debited (INR)", headerStyle);
            createCell(monthHeaderRow, 2, "Credited (INR)", headerStyle);
            createCell(monthHeaderRow, 3, "Total Activity (INR)", headerStyle);
            createCell(monthHeaderRow, 4, "Txn Count", headerStyle);

            if (summary.getMonthlyExpenses() != null) {
                for (Map.Entry<String, com.shashi.sms.dto.MonthlySummary> entry : summary.getMonthlyExpenses().entrySet()) {
                    Row row = summarySheet.createRow(rowIdx++);
                    com.shashi.sms.dto.MonthlySummary m = entry.getValue();
                    createCell(row, 0, entry.getKey(), dataStyle);
                    createCell(row, 1, m.getDebited(), currencyStyle);
                    createCell(row, 2, m.getCredited(), currencyStyle);
                    createCell(row, 3, m.getTotalAmount(), currencyStyle);
                    createCell(row, 4, (double) m.getTransactionCount(), dataStyle);
                }
            }

            // Sheet 2: All Transactions List
            Sheet txSheet = workbook.createSheet("All Transactions");
            int colCount = 11;
            String[] headers = {
                    "S.No", "Bank Name", "Account Number", "Type", "Amount (INR)",
                    "Date", "Time", "UPI / Ref No", "Source / Merchant", "Transaction ID", "Original Message"
            };

            Row txHeaderRow = txSheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                createCell(txHeaderRow, i, headers[i], headerStyle);
            }

            int txRowIdx = 1;
            for (int i = 0; i < summary.getTransactions().size(); i++) {
                BankTransaction txn = summary.getTransactions().get(i);
                Row row = txSheet.createRow(txRowIdx++);

                createCell(row, 0, (double) (i + 1), dataStyle);
                createCell(row, 1, txn.getBankName() != null ? txn.getBankName() : "", dataStyle);
                createCell(row, 2, txn.getAccount() != null ? txn.getAccount() : "", dataStyle);
                createCell(row, 3, txn.getTransactionType() != null ? txn.getTransactionType() : "", dataStyle);
                createCell(row, 4, txn.getAmount(), currencyStyle);
                createCell(row, 5, txn.getDate() != null ? txn.getDate().toString() : "", dataStyle);
                createCell(row, 6, txn.getTime() != null ? txn.getTime().toString() : "", dataStyle);
                createCell(row, 7, txn.getReference() != null ? txn.getReference() : "", dataStyle);
                createCell(row, 8, txn.getSource() != null ? txn.getSource() : "", dataStyle);
                createCell(row, 9, txn.getTransactionId() != null ? txn.getTransactionId() : "", dataStyle);
                createCell(row, 10, txn.getMessage() != null ? txn.getMessage() : "", dataStyle);
            }

            for (int i = 0; i < colCount; i++) {
                txSheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private SMSMessage parseSmsLine(String line) {
        String[] parts = line.split("\\|", -1);
        SMSMessage sms = new SMSMessage();
        if (parts.length >= 7) {
            sms.setSender(parts[0].trim());
            sms.setDate(parts[1].trim());
            sms.setRead("1".equals(parts[2].trim()) || "true".equalsIgnoreCase(parts[2].trim()));
            sms.setType(parts[3].trim());
            sms.setThread(parts[4].trim());
            sms.setService(parts[5].trim());
            sms.setMessage(parts[6].trim());
        } else if (parts.length == 1) {
            sms.setSender("Unknown");
            sms.setDate("");
            sms.setMessage(parts[0].trim());
        } else {
            return null;
        }
        return sms;
    }

    private CellStyle createTitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDataStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook wb) {
        CellStyle style = createDataStyle(wb);
        style.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private void createCell(Row row, int column, Object value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value instanceof Double) {
            cell.setCellValue((Double) value);
        } else if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        } else {
            cell.setCellValue(value != null ? value.toString() : "");
        }
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

}
