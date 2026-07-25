# 🏦 Message Expense Checker (Bank SMS Monthly Expense Tracker)

A **Spring Boot & Spring Batch** application designed to process, classify, analyze, and track financial transactions from bank SMS text logs. 

It uses **Spring Batch** to dynamically process uploaded SMS text files, filter out non-transactional messages (such as OTPs, promotional alerts, or generic notifications), extract financial details (amounts, banks, account numbers, dates, reference numbers), persist parsed transactions into **MongoDB**, and generate comprehensive JSON summaries and styled **Excel (`.xlsx`)** reports.

---

## 🛠️ Technology Stack

- **Java 17+**
- **Spring Boot 3.4.8**
- **Spring Batch 5** (Step-Scoped ItemReader, Chunk-Oriented Processing)
- **Spring Data MongoDB**
- **Apache POI** (Excel Report Generation)
- **Lombok** & **MapStruct**
- **Maven**

---

## ✨ Key Features

- **Spring Batch Execution**: Every uploaded file is saved locally to an `uploads/` directory and processed using a dynamic, step-scoped Spring Batch job (`smsMessageJob`).
- **Smart SMS Classification**: Identifies and extracts valid bank transactions while automatically filtering out non-transactional messages (OTP, statement requests, promotional text).
- **Comprehensive Financial Aggregation**:
  - Total Debited (Expenses) & Total Credited (Income / Refunds)
  - Message & Transaction Counts (Total Messages, Total Transactions, Debit Count, Credit Count)
  - Bank-wise Breakdown (Total Debited per bank & Total Credited per bank)
  - Monthly Summary (Debited, Credited, Total Activity, Transaction Count per month)
- **MongoDB Persistence**: Saves parsed `BankTransaction` documents directly to the `bank_transactions` MongoDB collection.
- **Excel Exporting**: Generates professional multi-sheet Excel reports with styled summary tables and itemized transaction audit logs.

---

## 📩 Sample Input Text Format

The application expects a pipe-delimited (`|`) text file containing raw SMS logs formatted as follows:

```txt
Sender|Date|Read|Type|Thread|Service|Message
VK-HDFCBK|15/05/2024 14:30|1|1|1|SMS|Rs 1500.00 debited from A/C **1234 on 15-May-24 via UPI Ref No 9876543210.
VK-ICICIB|16/05/2024 10:15|1|1|1|SMS|Rs 600.00 credited to A/C **5678 on 16-May-24 via UPI Ref No 1234567890.
VK-HDFCBK|17/05/2024 18:20|1|1|1|SMS|Rs 200.00 spent on A/C **1234 for mobile recharge. Available balance is Rs 5000.00
AD-HDFCBK|18/05/2024 11:00|1|1|1|SMS|Your OTP is 123456. Do not share it with anyone.
```

---

## 🚀 REST API Endpoints

### 1. Analyze SMS File (Get Summary JSON)

Processes an uploaded SMS text file via Spring Batch, saves transactions to MongoDB, and returns a high-level summary response.

- **URL**: `/api/expenses/analyze`
- **Method**: `POST`
- **Content-Type**: `multipart/form-data`
- **Request Parameter**:
  - `file` (*MultipartFile*, Required): The SMS text log file.

#### 📋 Example Expected Result (JSON Output):

```json
{
  "totalDebit": 1700.0,
  "totalCredit": 600.0,
  "totalMessagesCount": 4,
  "totalTransactionsCount": 3,
  "totalDebitCount": 2,
  "totalCreditCount": 1,
  "bankWiseExpenses": {
    "HDFC Bank": 1700.0
  },
  "bankWiseCredits": {
    "ICICI Bank": 600.0
  },
  "monthlyExpenses": {
    "2024-05 (May)": {
      "debited": 1700.0,
      "credited": 600.0,
      "totalAmount": 2300.0,
      "transactionCount": 3
    }
  }
}
```

---

### 2. Export Expense Excel Report

Processes an uploaded SMS text file via Spring Batch and generates a downloadable formatted Excel workbook.

- **URL**: `/api/expenses/export-excel`
- **Method**: `POST`
- **Content-Type**: `multipart/form-data`
- **Request Parameter**:
  - `file` (*MultipartFile*, Required): The SMS text log file.
- **Response**: Binary Excel file stream (`expense_analysis_report.xlsx`)

#### 📄 Excel Workbook Structure:

1. **Sheet 1: `Expense Summary`**
   - High-Level Summary Card Table (Total Transactions, Total Debits, Total Credits, Net Balance Change)
   - Bank-wise Deducted Breakdown Table
   - Bank-wise Received Breakdown Table
   - Monthly Breakdown Table (`Month` | `Debited (INR)` | `Credited (INR)` | `Total Activity (INR)` | `Txn Count`)

2. **Sheet 2: `All Transactions`**
   - Itemized table with columns: `S.No`, `Bank Name`, `Account Number`, `Type`, `Amount (INR)`, `Date`, `Time`, `UPI / Ref No`, `Source / Merchant`, `Transaction ID`, `Original Message`.

---

## ⚡ How to Run

### Prerequisites
- **Java 17+**
- **MongoDB** running on `localhost:27017` (or configured via `application.properties`)

### Commands

1. **Run Unit Tests**:
   ```bash
   ./mvnw.cmd clean test
   ```

2. **Start the Application**:
   ```bash
   ./mvnw.cmd spring-boot:run
   ```

The application will start on port `8080` (Default: `http://localhost:8080`).
