package com.shashi.sms.serviceImpl;

import java.io.BufferedWriter;
import java.io.FileWriter;

import org.springframework.stereotype.Service;

@Service
public class TransactionOutputServiceImpl {

    private BufferedWriter writer;

    public synchronized void openOutputFile() throws Exception {
        if (writer == null) {
            writer = new BufferedWriter(new FileWriter("allSmsBody.txt", true));
        }
    }

    public synchronized void closeOutputFile() throws Exception {
        if (writer != null) {
            try {
                writer.close();
            } finally {
                writer = null;
            }
        }
    }   

    public synchronized void writeLine(String line) throws Exception {
        if (writer != null) {
            writer.write(line);
            writer.newLine();
            writer.flush();
        }
    }
    
}
