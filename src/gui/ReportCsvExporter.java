package gui;

import java.awt.Component;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Shared CSV export for the Manager reports.
 * Exports the currently displayed JTable rows, respecting sorting and active filters.
 * Does not export hidden user data such as passwords.
 */
public final class ReportCsvExporter {
    private ReportCsvExporter() { }

    public static void export(Component parent, JTable table, String suggestedFileName) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export report to CSV");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV files (*.csv)", "csv"));
        chooser.setSelectedFile(new java.io.File(suggestedFileName));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;

        Path output = chooser.getSelectedFile().toPath();
        if (!output.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".csv")) {
            output = output.resolveSibling(output.getFileName().toString() + ".csv");
        }
        if (Files.exists(output)) {
            int result = JOptionPane.showConfirmDialog(parent,
                    "Overwrite existing file?\n" + output,
                    "Confirm export", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (result != JOptionPane.YES_OPTION) return;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            // Use the visible column order and visible (possibly sorted) rows.
            for (int column = 0; column < table.getColumnCount(); column++) {
                if (column > 0) writer.write(',');
                writer.write(csvValue(table.getColumnName(column)));
            }
            writer.newLine();
            for (int row = 0; row < table.getRowCount(); row++) {
                for (int column = 0; column < table.getColumnCount(); column++) {
                    if (column > 0) writer.write(',');
                    Object value = table.getValueAt(row, column);
                    writer.write(csvValue(value == null ? "" : value.toString()));
                }
                writer.newLine();
            }
            JOptionPane.showMessageDialog(parent,
                    "Exported " + table.getRowCount() + " rows to:\n" + output,
                    "CSV export complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent,
                    "Could not export report: " + ex.getMessage(),
                    "Export failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String csvValue(String text) {
        // Prevent spreadsheet programs from interpreting untrusted text as a formula.
        if (!text.isEmpty() && "=+@".indexOf(text.charAt(0)) >= 0) text = "'" + text;
        return '"' + text.replace("\"", "\"\"") + '"';
    }
}
