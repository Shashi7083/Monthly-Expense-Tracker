package com.shashi.sms.serviceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import com.shashi.sms.dto.ExpenseSummaryResponse;

@SpringBootTest
public class ExpenseAnalysisServiceImplTest {

    @Autowired
    private ExpenseAnalysisServiceImpl expenseAnalysisService;

    @Test
    void testAnalyzeSmsFile() throws Exception {
        String smsContent = "Sender|Date|Read|Type|Thread|Service|Message\n" +
                "VK-HDFCBK|15/05/2024 14:30|1|1|1|SMS|Rs 1500.00 debited from A/C **1234 on 15-May-24 via UPI Ref No 9876543210.\n"
                +
                "VK-ICICIB|16/05/2024 10:15|1|1|1|SMS|Rs 500.00 credited to A/C **5678 on 16-May-24 via UPI Ref No 1234567890.\n"
                +
                "AD-HDFCBK|17/05/2024 11:00|1|1|1|SMS|Your OTP is 123456. Do not share it with anyone.";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sms_log.txt",
                "text/plain",
                smsContent.getBytes());

        ExpenseSummaryResponse summary = expenseAnalysisService.analyzeSmsFile(file);

        assertNotNull(summary);
        assertEquals(2, summary.getTotalTransactionsCount()); // 2 valid transactions (1 Debit + 1 Credit)
        assertEquals(1500.0, summary.getTotalDebit());
        assertEquals(500.0, summary.getTotalCredit());
        assertTrue(summary.getBankWiseExpenses().containsKey("HDFC Bank"));
        assertEquals(1500.0, summary.getBankWiseExpenses().get("HDFC Bank"));
        assertTrue(summary.getBankWiseCredits().containsKey("ICICI Bank"));
        assertEquals(500.0, summary.getBankWiseCredits().get("ICICI Bank"));
    }

    @Test
    void testExportExpenseExcel() throws Exception {
        String smsContent = "Sender|Date|Read|Type|Thread|Service|Message\n" +
                "VK-HDFCBK|15/05/2024 14:30|1|1|1|SMS|Rs 2500.00 debited from A/C **1234 via UPI.";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sms_log.txt",
                "text/plain",
                smsContent.getBytes());

        byte[] excelBytes = expenseAnalysisService.exportExpenseExcel(file);
        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0);
    }

    @Test
    void testAnalyzeSmsFileWithAlternativeKeywords() throws Exception {
        String smsContent = "Sender|Date|Read|Type|Thread|Service|Message\n" +
                "VK-HDFCBK|15/05/2024 14:30|1|1|1|SMS|Rs 200.00 spent on A/C **1234 for mobile recharge. Available balance is Rs 5000.00\n" +
                "VK-ICICIB|16/05/2024 10:15|1|1|1|SMS|Rs 100.00 refunded to A/C **5678 on 16-May-24.";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sms_log.txt",
                "text/plain",
                smsContent.getBytes());

        ExpenseSummaryResponse summary = expenseAnalysisService.analyzeSmsFile(file);

        assertNotNull(summary);
        assertEquals(2, summary.getTotalTransactionsCount());
        assertEquals(200.0, summary.getTotalDebit());
        assertEquals(100.0, summary.getTotalCredit());
    }

}
