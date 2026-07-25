package com.shashi.sms.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.shashi.sms.model.BankTransaction;
import com.shashi.sms.model.SMSMessage;
import com.shashi.sms.service.TransactionService;

@Service
public class TransactionServiceImpl implements TransactionService {

    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "(?i)(?:INR|Rs|Rs\\.|₹)[:\\s]?([0-9]+(?:,[0-9]+)*(?:\\.[0-9]{1,2})?)");

    private static final Pattern UPI_PATTERN = Pattern.compile("(?i)(?:UPI|ref no|Ref No)[ :]*([0-9]+)");

    private static final Pattern TRANSACTION_ID_PATTERN = Pattern.compile(
            "(?i)(?:(?:Txn|Transaction|Ref|Reference)[\\s:-]*(?:ID)?[:\\s#]*)([A-Za-z0-9]{6,})");

    private static final Map<String, String> BANK_PATTERNS = new LinkedHashMap<>();

    static {
        BANK_PATTERNS.put("hdfc", "HDFC Bank");
        BANK_PATTERNS.put("icici", "ICICI Bank");
        BANK_PATTERNS.put("axis", "Axis Bank");
        BANK_PATTERNS.put("kotak", "Kotak Mahindra Bank");
        BANK_PATTERNS.put("indusind", "IndusInd Bank");
        BANK_PATTERNS.put("yes bank", "YES Bank");
        BANK_PATTERNS.put("federal", "Federal Bank");
        BANK_PATTERNS.put("rbl", "RBL Bank");
        BANK_PATTERNS.put("idfc", "IDFC FIRST Bank");
        BANK_PATTERNS.put("bandhan", "Bandhan Bank");
        BANK_PATTERNS.put("au bank", "AU Small Finance Bank");
        BANK_PATTERNS.put("au small", "AU Small Finance Bank");
        BANK_PATTERNS.put("csb", "CSB Bank");
        BANK_PATTERNS.put("catholic syrian", "CSB Bank");
        BANK_PATTERNS.put("city union", "City Union Bank");
        BANK_PATTERNS.put("dcb", "DCB Bank");
        BANK_PATTERNS.put("dhanlaxmi", "Dhanlaxmi Bank");
        BANK_PATTERNS.put("j&k", "Jammu & Kashmir Bank");
        BANK_PATTERNS.put("jammu & kashmir", "Jammu & Kashmir Bank");
        BANK_PATTERNS.put("karnataka bank", "Karnataka Bank");
        BANK_PATTERNS.put("karur vysya", "Karur Vysya Bank");
        BANK_PATTERNS.put("kvb", "Karur Vysya Bank");
        BANK_PATTERNS.put("nainital", "Nainital Bank");
        BANK_PATTERNS.put("south indian", "South Indian Bank");
        BANK_PATTERNS.put("sib", "South Indian Bank");
        BANK_PATTERNS.put("tamilnad mercantile", "Tamilnad Mercantile Bank");
        BANK_PATTERNS.put("tmb", "Tamilnad Mercantile Bank");
        BANK_PATTERNS.put("equitas", "Equitas Small Finance Bank");
        BANK_PATTERNS.put("ujjivan", "Ujjivan Small Finance Bank");
        BANK_PATTERNS.put("esaf", "ESAF Small Finance Bank");
        BANK_PATTERNS.put("fincare", "Fincare Small Finance Bank");
        BANK_PATTERNS.put("suryoday", "Suryoday Small Finance Bank");
        BANK_PATTERNS.put("capital small", "Capital Small Finance Bank");
        BANK_PATTERNS.put("north east small", "North East Small Finance Bank");
        BANK_PATTERNS.put("jana small", "Jana Small Finance Bank");
        BANK_PATTERNS.put("shivalik", "Shivalik Small Finance Bank");
        BANK_PATTERNS.put("unity small", "Unity Small Finance Bank");
        BANK_PATTERNS.put("utkarsh", "Utkarsh Small Finance Bank");

        // Foreign Banks commonly present in India
        BANK_PATTERNS.put("citibank", "Citi Bank");
        BANK_PATTERNS.put("citi", "Citi Bank");
        BANK_PATTERNS.put("hsbc", "HSBC Bank");
        BANK_PATTERNS.put("standard chartered", "Standard Chartered Bank");
        BANK_PATTERNS.put("scb", "Standard Chartered Bank");
        BANK_PATTERNS.put("stanc", "Standard Chartered Bank");
        BANK_PATTERNS.put("dbs", "DBS Bank");
        BANK_PATTERNS.put("digibank", "DBS Bank");
        BANK_PATTERNS.put("deutsche", "Deutsche Bank");
        BANK_PATTERNS.put("barclays", "Barclays Bank");
        BANK_PATTERNS.put("amex", "American Express");
        BANK_PATTERNS.put("american express", "American Express");

        // Public Sector
        BANK_PATTERNS.put("sbi", "State Bank of India");
        BANK_PATTERNS.put("state bank of india", "State Bank of India"); // In case 'sbi' is too short? But safe here.
        BANK_PATTERNS.put("pnb", "Punjab National Bank");
        BANK_PATTERNS.put("punjab national", "Punjab National Bank");
        BANK_PATTERNS.put("bob", "Bank of Baroda");
        BANK_PATTERNS.put("b o b", "Bank of Baroda");
        BANK_PATTERNS.put("bank of baroda", "Bank of Baroda");
        BANK_PATTERNS.put("canara", "Canara Bank");
        BANK_PATTERNS.put("union bank", "Union Bank of India"); // Generic 'union bank' put after 'city union'
        BANK_PATTERNS.put("indian bank", "Indian Bank");
        BANK_PATTERNS.put("bank of india", "Bank of India");
        BANK_PATTERNS.put("boi", "Bank of India");
        BANK_PATTERNS.put("central bank", "Central Bank of India");
        BANK_PATTERNS.put("cboi", "Central Bank of India");
        BANK_PATTERNS.put("indian overseas", "Indian Overseas Bank");
        BANK_PATTERNS.put("iob", "Indian Overseas Bank");
        BANK_PATTERNS.put("uco", "UCO Bank");
        BANK_PATTERNS.put("punjab & sind", "Punjab & Sind Bank");
        BANK_PATTERNS.put("psb", "Punjab & Sind Bank");
        BANK_PATTERNS.put("bank of maharashtra", "Bank of Maharashtra");
        BANK_PATTERNS.put("mahagram", "Bank of Maharashtra");
        BANK_PATTERNS.put("idbi", "IDBI Bank");

        // Payments Banks
        BANK_PATTERNS.put("paytm", "Paytm Payments Bank");
        BANK_PATTERNS.put("airtel money", "Airtel Payments Bank");
        BANK_PATTERNS.put("airtel bank", "Airtel Payments Bank");
        BANK_PATTERNS.put("jio money", "Jio Payments Bank");
        BANK_PATTERNS.put("jio bank", "Jio Payments Bank");
        BANK_PATTERNS.put("ippb", "India Post Payments Bank");
        BANK_PATTERNS.put("india post", "India Post Payments Bank");
        BANK_PATTERNS.put("fino", "Fino Payments Bank");
        BANK_PATTERNS.put("nsdl", "NSDL Payments Bank");

        // Cooperative Banks
        BANK_PATTERNS.put("saraswat", "Saraswat Co-operative Bank");
        BANK_PATTERNS.put("shamrao", "SVC Co-operative Bank");
        BANK_PATTERNS.put("svc", "SVC Co-operative Bank");
        BANK_PATTERNS.put("cosmos", "Cosmos Co-operative Bank");
        BANK_PATTERNS.put("abhyudaya", "Abhyudaya Co-operative Bank");
        BANK_PATTERNS.put("janata sahakari", "Janata Sahakari Bank");
        BANK_PATTERNS.put("nkgsb", "NKGSB Co-operative Bank");
        BANK_PATTERNS.put("ap mahesh", "AP Mahesh Co-operative Urban Bank");
        BANK_PATTERNS.put("kalupur", "Kalupur Commercial Co-operative Bank");
        BANK_PATTERNS.put("msc bank", "Maharashtra State Co-operative Bank");
        BANK_PATTERNS.put("citizen credit", "Citizen Credit Co-operative Bank");
        BANK_PATTERNS.put("tjsb", "TJSB Sahakari Bank");
        BANK_PATTERNS.put("kalyan janata", "Kalyan Janata Sahakari Bank");
        BANK_PATTERNS.put("dombivli nagari", "Dombivli Nagari Sahakari Bank");
        // Add more as needed
    }

    @Override
    public BankTransaction transaction(SMSMessage smsMessage) throws Exception {

        BankTransaction bankTransaction = new BankTransaction();
        String message = smsMessage.getMessage();

        bankTransaction.setSender(smsMessage.getSender());
        bankTransaction.setMessage(message);

        bankTransaction.setBankName(detectBankName(smsMessage.getSender(), smsMessage.getMessage()));

        Matcher accMatcher = Pattern.compile("Acct?\\s+([\\w*Xx0-9]+)|A/c\\s+\\*?([\\w0-9]+)").matcher(message);
        if (accMatcher.find()) {
            bankTransaction.setAccount(accMatcher.group(1) != null ? accMatcher.group(1) : accMatcher.group(2));
        }

        String lowerMsg = message.toLowerCase();
        if (lowerMsg.contains("debited") || lowerMsg.contains("spent") || lowerMsg.contains("withdrawn")
                || lowerMsg.contains("purchase") || lowerMsg.contains("deducted") || lowerMsg.contains("sent")
                || lowerMsg.contains("payment of")) {
            bankTransaction.setTransactionType("Debit");
        } else if (lowerMsg.contains("credited") || lowerMsg.contains("refunded") || lowerMsg.contains("received")) {
            bankTransaction.setTransactionType("Credit");
        } else {
            bankTransaction.setTransactionType("Debit");
        }

        Matcher amountMatcher = AMOUNT_PATTERN.matcher(message);
        if (amountMatcher.find()) {
            String amountStr = amountMatcher.group(1).replace(",", "");
            bankTransaction.setAmount(Double.parseDouble(amountStr));
        }

        String rawDateTime = smsMessage.getDate();
        LocalDateTime dateTime = parseDateTime(rawDateTime, message);
        bankTransaction.setDate(dateTime.toLocalDate());
        bankTransaction.setTime(dateTime.toLocalTime());

        // UPI / Reference
        Matcher upiMatcher = UPI_PATTERN.matcher(message);
        if (upiMatcher.find()) {
            bankTransaction.setReference(upiMatcher.group(1));
        }

        // Source (who/where money went or came from)
        Matcher sourceMatcher = Pattern
                .compile("for\\s+([\\w\\s.&-]+)[.]|;\\s*([\\w\\s.&-]+)\\s+credited", Pattern.CASE_INSENSITIVE)
                .matcher(message);
        if (sourceMatcher.find()) {
            bankTransaction.setSource(
                    sourceMatcher.group(1) != null ? sourceMatcher.group(1).trim() : sourceMatcher.group(2).trim());
        }

        Matcher txnIdMatcher = TRANSACTION_ID_PATTERN.matcher(message);
        if (txnIdMatcher.find()) {
            bankTransaction.setTransactionId(txnIdMatcher.group(1).trim());
        }

        return bankTransaction;
    }

    @Override
    public boolean isBankTransaction(SMSMessage smsMessage) throws Exception {
        if (smsMessage == null || smsMessage.getMessage() == null)
            return false;

        String msg = smsMessage.getMessage().toLowerCase();

        // Exclude non-transactional messages
        String[] nonTransactionKeywords = {
                "otp", "one time password", "do not share", "loan", "emi", "due on",
                "statement", "thanks for", "thank you", "limit", "offer", "sale",
                "get free", "pay later", "secured", "received your request", "not a txn",
                "avoid sharing", "new pin", "card delivery", "welcome to", "dear customer", "no txn"
        };
        for (String keyword : nonTransactionKeywords) {
            if (msg.contains(keyword))
                return false;
        }

        // Ensure it contains currency + amount (Rs./INR/₹)
        Pattern amountPattern = Pattern.compile(
                "(?i)(?:INR|Rs|Rs\\.|₹)[:\\s]?([0-9]+(?:,[0-9]+)*(?:\\.[0-9]{1,2})?)");
        boolean hasAmount = amountPattern.matcher(msg).find();

        // Look for transaction-type keywords
        String[] transactionKeywords = {
                "debited", "credited", "spent", "payment of", "purchase", "txn", "withdrawn",
                "received", "sent", "upi txn", "imps", "neft", "rtgs", "transaction of", "deducted", "refunded"
        };
        boolean hasTxnKeyword = Arrays.stream(transactionKeywords).anyMatch(msg::contains);

        return hasAmount && hasTxnKeyword;
    }

    @Override
    public String detectBankName(String sender, String message) {
        String lowerSender = sender == null ? "" : sender.toLowerCase();
        String lowerMsg = message == null ? "" : message.toLowerCase();

        for (Map.Entry<String, String> entry : BANK_PATTERNS.entrySet()) {
            String pattern = entry.getKey();
            String bankName = entry.getValue();
            if (lowerSender.contains(pattern) || lowerMsg.contains(pattern)) {
                return bankName;
            }
        }

        return "Unknown Bank";
    }

    private LocalDateTime parseDateTime(String rawDateTime, String message) {
        if (rawDateTime != null && !rawDateTime.trim().isEmpty()) {
            String[] patterns = {
                    "dd/MM/yyyy HH:mm",
                    "dd-MM-yyyy HH:mm",
                    "yyyy-MM-dd HH:mm",
                    "dd/MM/yyyy",
                    "dd-MM-yyyy",
                    "yyyy-MM-dd",
                    "dd-MMM-yyyy",
                    "dd-MMM-yy"
            };

            for (String pattern : patterns) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, java.util.Locale.ENGLISH);
                    if (pattern.contains("HH:mm")) {
                        return LocalDateTime.parse(rawDateTime.trim(), formatter);
                    } else {
                        LocalDate date = LocalDate.parse(rawDateTime.trim(), formatter);
                        return date.atStartOfDay();
                    }
                } catch (Exception ignored) {
                }
            }
        }

        // Try extracting date from message text (e.g. "on 15-May-24", "on 15/05/2024",
        // "on 15-05-24")
        if (message != null) {
            Matcher msgDateMatcher = Pattern.compile("(?i)on\\s+([0-9]{1,2}[/-][A-Za-z0-9]{2,3}[/-][0-9]{2,4})")
                    .matcher(message);
            if (msgDateMatcher.find()) {
                String dateStr = msgDateMatcher.group(1);
                String[] msgDatePatterns = { "dd-MMM-yy", "dd-MMM-yyyy", "dd/MM/yyyy", "dd-MM-yyyy", "dd/MM/yy",
                        "dd-MM-yy" };
                for (String pattern : msgDatePatterns) {
                    try {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, java.util.Locale.ENGLISH);
                        LocalDate date = LocalDate.parse(dateStr, formatter);
                        return date.atStartOfDay();
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        // Fallback to current date/time if date is missing or unparseable
        return LocalDateTime.now();
    }

}
