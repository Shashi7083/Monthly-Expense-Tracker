package com.shashi.sms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.shashi.sms.dto.ExpenseSummaryResponse;
import com.shashi.sms.service.ExpenseAnalysisService;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseAnalysisService expenseAnalysisService;

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeExpenses(@RequestParam("file") MultipartFile file) {
        try {
            ExpenseSummaryResponse summary = expenseAnalysisService.analyzeSmsFile(file);
            return ResponseEntity.ok(summary);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error analyzing SMS file: " + e.getMessage());
        }
    }

    @PostMapping("/export-excel")
    public ResponseEntity<?> exportExpenseExcel(@RequestParam("file") MultipartFile file) {
        try {
            byte[] excelBytes = expenseAnalysisService.exportExpenseExcel(file);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(
                    MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "expense_analysis_report.xlsx");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error generating Excel report: " + e.getMessage());
        }
    }

}
