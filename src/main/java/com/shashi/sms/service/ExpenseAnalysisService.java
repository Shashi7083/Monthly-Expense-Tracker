package com.shashi.sms.service;

import org.springframework.web.multipart.MultipartFile;

import com.shashi.sms.dto.ExpenseSummaryResponse;

public interface ExpenseAnalysisService {

    ExpenseSummaryResponse analyzeSmsFile(MultipartFile file) throws Exception;

    byte[] exportExpenseExcel(MultipartFile file) throws Exception;

}
