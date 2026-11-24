package gui;

import java.awt.Component;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Saves the rows a table is showing (after searching and sorting) to a CSV file.
 * Used by Attendance Records and Attendance Report.
 */
public final class AttendanceCsvExporter {
    private AttendanceCsvExporter() { }

    public static void export(Component parent, JTable table, String suggestedFileName) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export to CSV");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV files (*.csv)", "csv"));
        chooser.setSelectedFile(new File(suggestedFileName));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;

        Path output = chooser.getSelectedFile().toPath();
        if (!output.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".csv")) {
            output = output.resolveSibling(output.getFileName().toString() + ".csv");
        }
        if (Files.exists(output)) {
            int answer = JOptionPane.showConfirmDialog(parent, "Overwrite existing file?\n" + output,
                    "Confirm export", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (answer != JOptionPane.YES_OPTION) return;
        }

        try (BufferedWriter out = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            for (int c = 0; c < table.getColumnCount(); c++) {
                if (c > 0) out.write(',');
                out.write(quote(table.getColumnName(c)));
            }
            out.newLine();
            for (int r = 0; r < table.getRowCount(); r++) {
                for (int c = 0; c < table.getColumnCount(); c++) {
                    if (c > 0) out.write(',');
                    Object v = table.getValueAt(r, c);
                    out.write(quote(v == null ? "" : v.toString()));
                }
                out.newLine();
            }
            JOptionPane.showMessageDialog(parent, "Exported " + table.getRowCount() + " row(s) to:\n" + output,
                    "Export complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent, "Could not export: " + ex.getMessage(),
                    "Export failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Wraps text in quotes; a leading = + - @ is neutralised so Excel does not run it as a formula. */
    private static String quote(String text) {
        if (!text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0) text = "'" + text;
        return '"' + text.replace("\"", "\"\"") + '"';
    }
}
