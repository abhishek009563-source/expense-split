package com.expensesplitter.util;

import com.expensesplitter.model.Expense;
import com.expensesplitter.model.Settlement;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class CsvExporter {

    public static void exportExpensesToCsv(File file, List<Expense> expenses) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            // Header
            writer.println("Expense ID,Date,Title,Group,Paid By,Category,Split Method,Amount (INR),Description");
            for (Expense e : expenses) {
                writer.printf("%d,%s,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%.2f,\"%s\"%n",
                        e.getId(),
                        e.getExpenseDate() != null ? e.getExpenseDate().toString() : "",
                        escapeCsv(e.getTitle()),
                        escapeCsv(e.getGroupName()),
                        escapeCsv(e.getPaidByUserName()),
                        e.getCategory() != null ? e.getCategory().getDisplayName() : "",
                        e.getSplitMethod() != null ? e.getSplitMethod().getDisplayName() : "",
                        e.getAmountPaise() / 100.0,
                        escapeCsv(e.getDescription() != null ? e.getDescription() : "")
                );
            }
        }
    }

    public static void exportSettlementsToCsv(File file, List<Settlement> settlements) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("Settlement ID,Date,Group,Paid By,Received By,Amount (INR),Notes");
            for (Settlement s : settlements) {
                writer.printf("%d,%s,\"%s\",\"%s\",\"%s\",%.2f,\"%s\"%n",
                        s.getId(),
                        s.getSettlementDate() != null ? s.getSettlementDate().toString() : "",
                        escapeCsv(s.getGroupName()),
                        escapeCsv(s.getPayerName()),
                        escapeCsv(s.getReceiverName()),
                        s.getAmountPaise() / 100.0,
                        escapeCsv(s.getNotes() != null ? s.getNotes() : "")
                );
            }
        }
    }

    private static String escapeCsv(String str) {
        if (str == null) return "";
        return str.replace("\"", "\"\"");
    }
}

