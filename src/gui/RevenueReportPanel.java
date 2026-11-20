package gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import service.RevenueReportService;
import service.RevenueReportService.BreakdownRow;
import service.RevenueReportService.ClassRevenueRow;
import service.RevenueReportService.MonthRevenue;
import service.RevenueReportService.Report;
import util.DataException;

/**
 * MG3: read-only revenue report with class-level rows, custom date filters,
 * monthly chart, growth comparisons, payment-method and martial-art breakdowns,
 * and CSV export. All revenue calculations live in RevenueReportService.
 */
public class RevenueReportPanel extends JPanel {
    private final RevenueReportService service = new RevenueReportService();
    private final JComboBox<String> cmbMode = new JComboBox<>(new String[]{"Monthly", "Yearly"});
    private final JComboBox<String> cmbYear = new JComboBox<>();
    private final JComboBox<String> cmbMonth = new JComboBox<>(new String[]{
            "All months", "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    });
    private final JTextField txtFrom = new JTextField(10);
    private final JTextField txtTo = new JTextField(10);
    private final StatCard registrationsCard = new StatCard("Paid registrations");
    private final StatCard revenueCard = new StatCard("Total revenue");
    private final StatCard growthCard = new StatCard("Revenue growth");
    private final JLabel comparisonLabel = new JLabel(" ");
    private final JLabel lblMessage = new JLabel(" ");

    // Main report: one row per reporting period AND class.
    private final SortableTable classTable = new SortableTable(
            "Period", "Class", "Martial Art", "Registrations", "Revenue (RM)");
    private final SortableTable methodTable = new SortableTable(
            "Payment Method", "Registrations", "Revenue (RM)");
    private final SortableTable disciplineTable = new SortableTable(
            "Martial Art", "Registrations", "Revenue (RM)");
    private final MonthlyBarChart monthlyChart = new MonthlyBarChart();
    private final JTabbedPane tabs = new JTabbedPane();
    private boolean dataLoaded;

    public RevenueReportPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        buildInterface();
        loadYears();
        cmbMode.addActionListener(e -> cmbMonth.setEnabled(cmbMode.getSelectedIndex() == 0));
        addComponentListener(new ComponentAdapter() {
            @Override public void componentShown(ComponentEvent e) {
                loadYears();
                refresh();
            }
        });
        refresh();
    }

    private void buildInterface() {
        JLabel title = new JLabel("Revenue Report");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        JLabel subtitle = new JLabel("Monthly / yearly paid registrations and revenue by class");
        subtitle.setForeground(Color.DARK_GRAY);
        JPanel heading = new JPanel(new GridLayout(0, 1, 0, 3));
        heading.add(title);
        heading.add(subtitle);

        JButton apply = new JButton("Apply");
        JButton reset = new JButton("Reset");
        JButton export = new JButton("Export CSV");
        JPanel firstFilters = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 1));
        firstFilters.add(new JLabel("Report"));
        firstFilters.add(cmbMode);
        firstFilters.add(new JLabel("Year"));
        firstFilters.add(cmbYear);
        firstFilters.add(new JLabel("Month"));
        firstFilters.add(cmbMonth);
        firstFilters.add(apply);
        firstFilters.add(reset);

        txtFrom.setToolTipText("Inclusive starting payment date: yyyy-MM-dd");
        txtTo.setToolTipText("Inclusive ending payment date: yyyy-MM-dd");
        JPanel secondFilters = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 1));
        secondFilters.add(new JLabel("Paid from (yyyy-MM-dd)"));
        secondFilters.add(txtFrom);
        secondFilters.add(new JLabel("To"));
        secondFilters.add(txtTo);
        secondFilters.add(export);

        apply.addActionListener(e -> refresh());
        reset.addActionListener(e -> resetFilters());
        txtFrom.addActionListener(e -> refresh());
        txtTo.addActionListener(e -> refresh());
        export.addActionListener(e -> exportReport());

        JPanel filters = new JPanel();
        filters.setLayout(new BoxLayout(filters, BoxLayout.Y_AXIS));
        firstFilters.setAlignmentX(LEFT_ALIGNMENT);
        secondFilters.setAlignmentX(LEFT_ALIGNMENT);
        filters.add(firstFilters);
        filters.add(secondFilters);

        JPanel cards = new JPanel(new GridLayout(1, 3, 8, 0));
        cards.add(registrationsCard);
        cards.add(revenueCard);
        cards.add(growthCard);

        JPanel north = new JPanel(new BorderLayout(0, 7));
        JPanel top = new JPanel(new BorderLayout(0, 7));
        top.add(heading, BorderLayout.NORTH);
        top.add(filters, BorderLayout.CENTER);
        north.add(top, BorderLayout.NORTH);
        north.add(cards, BorderLayout.CENTER);
        comparisonLabel.setForeground(Color.DARK_GRAY);
        north.add(comparisonLabel, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);

        classTable.setColumnWidths(100, 105, 120, 110, 130);
        methodTable.setColumnWidths(160, 130, 150);
        disciplineTable.setColumnWidths(160, 130, 150);
        tabs.addTab("Revenue by Class", new JScrollPane(classTable));
        tabs.addTab("Payment Methods", new JScrollPane(methodTable));
        tabs.addTab("Martial Arts", new JScrollPane(disciplineTable));
        tabs.addTab("Monthly Chart", new JScrollPane(monthlyChart));
        add(tabs, BorderLayout.CENTER);

        lblMessage.setForeground(Color.DARK_GRAY);
        add(lblMessage, BorderLayout.SOUTH);
    }

    /** Preserve the selected year while reloading the available years from payment records. */
    private void loadYears() {
        String previous = (String) cmbYear.getSelectedItem();
        cmbYear.removeAllItems();
        cmbYear.addItem("All years");
        for (Integer year : service.availableYears()) cmbYear.addItem(String.valueOf(year));
        if (previous != null) {
            for (int i = 0; i < cmbYear.getItemCount(); i++) {
                if (previous.equals(cmbYear.getItemAt(i))) {
                    cmbYear.setSelectedIndex(i);
                    return;
                }
            }
        }
        cmbYear.setSelectedIndex(0);
    }

    private void refresh() {
        LocalDate from;
        LocalDate to;
        try {
            from = parseDate(txtFrom.getText(), "From date");
            to = parseDate(txtTo.getText(), "To date");
            if (from != null && to != null && from.isAfter(to)) {
                dataLoaded = false;
                showError("From date cannot be after To date. Report is unchanged.");
                return;
            }
        } catch (IllegalArgumentException ex) {
            dataLoaded = false;
            showError(ex.getMessage() + " Report is unchanged.");
            return;
        }
        boolean monthly = cmbMode.getSelectedIndex() == 0;
        String selectedYear = (String) cmbYear.getSelectedItem();
        Integer year = selectedYear == null || "All years".equals(selectedYear)
                ? null : Integer.valueOf(selectedYear);
        Integer month = monthly && cmbMonth.getSelectedIndex() > 0
                ? cmbMonth.getSelectedIndex() : null;

        try {
            Report report = service.generateReport(monthly, year, month, from, to);
            classTable.clearRows();
            for (ClassRevenueRow row : report.getClasses()) {
                classTable.addRow(row.getPeriod(), row.getClassId(), row.getDiscipline(),
                        row.getRegistrations(), row.getRevenue().doubleValue());
            }
            methodTable.clearRows();
            for (BreakdownRow row : report.getMethods()) {
                methodTable.addRow(row.getCategory(), row.getRegistrations(),
                        row.getRevenue().doubleValue());
            }
            disciplineTable.clearRows();
            for (BreakdownRow row : report.getDisciplines()) {
                disciplineTable.addRow(row.getCategory(), row.getRegistrations(),
                        row.getRevenue().doubleValue());
            }
            monthlyChart.setRows(report.getMonthlyRevenue());
            registrationsCard.setValue(Integer.toString(report.getRegistrations()));
            revenueCard.setValue(money(report.getRevenue()));
            growthCard.setValue(report.getGrowthPercent() == null ? "N/A"
                    : String.format(Locale.US, "%+.1f%%", report.getGrowthPercent()));
            comparisonLabel.setText("Comparison: " + report.getComparison());
            lblMessage.setForeground(Color.DARK_GRAY);
            lblMessage.setText(report.getClasses().isEmpty() ? "No revenue records for this filter."
                    : report.getClasses().size() + " class / period rows shown");
            dataLoaded = true;
        } catch (DataException ex) {
            dataLoaded = false;
            classTable.clearRows();
            methodTable.clearRows();
            disciplineTable.clearRows();
            monthlyChart.setRows(new ArrayList<>());
            registrationsCard.setValue("0");
            revenueCard.setValue("RM 0.00");
            growthCard.setValue("N/A");
            comparisonLabel.setText(" ");
            showError("Could not read revenue records: " + ex.getMessage());
        }
    }

    private static LocalDate parseDate(String value, String label) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(label + " must be a valid yyyy-MM-dd date.");
        }
    }

    private void showError(String message) {
        lblMessage.setForeground(new Color(185, 0, 0));
        lblMessage.setText(message);
    }

    private void resetFilters() {
        cmbMode.setSelectedIndex(0);
        cmbYear.setSelectedIndex(0);
        cmbMonth.setSelectedIndex(0);
        txtFrom.setText("");
        txtTo.setText("");
        refresh();
    }

    /** Export the selected table, or the class report when the chart tab is selected. */
    private void exportReport() {
        if (!dataLoaded) return;
        int tab = tabs.getSelectedIndex();
        SortableTable selected = tab == 1 ? methodTable : tab == 2 ? disciplineTable : classTable;
        String file = tab == 1 ? "sdams_revenue_by_method.csv"
                : tab == 2 ? "sdams_revenue_by_martial_art.csv"
                : "sdams_revenue_by_class.csv";
        ReportCsvExporter.export(this, selected, file);
    }

    private static String money(BigDecimal value) {
        return String.format(Locale.US, "RM %,.2f", value.doubleValue());
    }

    /** A small pure-Swing bar chart; no third-party chart libraries required. */
    private static final class MonthlyBarChart extends JPanel {
        private List<MonthRevenue> rows = new ArrayList<>();

        MonthlyBarChart() {
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(580, 250));
        }

        void setRows(List<MonthRevenue> values) {
            rows = new ArrayList<>(values);
            setPreferredSize(new Dimension(Math.max(580, 95 + rows.size() * 76), 250));
            revalidate();
            repaint();
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                int left = 65;
                int right = getWidth() - 22;
                int top = 24;
                int bottom = getHeight() - 52;
                int height = Math.max(1, bottom - top);
                if (rows.isEmpty()) {
                    g.setColor(Color.DARK_GRAY);
                    g.drawString("No monthly revenue data for the selected filters.", 22, 35);
                    return;
                }
                double highest = 0.0;
                for (MonthRevenue item : rows) {
                    highest = Math.max(highest, item.getRevenue().doubleValue());
                }
                highest = Math.max(1.0, highest);
                g.setColor(new Color(214, 220, 229));
                g.setStroke(new BasicStroke(1f));
                for (int step = 0; step <= 4; step++) {
                    int y = bottom - step * height / 4;
                    g.drawLine(left, y, right, y);
                    String value = String.format(Locale.US, "%.0f", highest * step / 4);
                    FontMetrics metrics = g.getFontMetrics();
                    g.setColor(Color.DARK_GRAY);
                    g.drawString(value, left - metrics.stringWidth(value) - 8, y + 4);
                    g.setColor(new Color(214, 220, 229));
                }
                int slotWidth = (right - left) / rows.size();
                for (int i = 0; i < rows.size(); i++) {
                    MonthRevenue row = rows.get(i);
                    int barHeight = (int) Math.round(row.getRevenue().doubleValue() / highest * height);
                    int width = Math.min(42, Math.max(12, slotWidth - 18));
                    int x = left + slotWidth * i + (slotWidth - width) / 2;
                    int y = bottom - barHeight;
                    g.setColor(new Color(54, 105, 179));
                    g.fillRoundRect(x, y, width, barHeight, 6, 6);
                    g.setColor(Color.DARK_GRAY);
                    String month = row.getLabel();
                    FontMetrics fm = g.getFontMetrics();
                    g.drawString(month, left + slotWidth * i + (slotWidth - fm.stringWidth(month)) / 2,
                            bottom + 18);
                    String amount = String.format(Locale.US, "%.0f", row.getRevenue().doubleValue());
                    g.drawString(amount, x + (width - fm.stringWidth(amount)) / 2, Math.max(top - 6, y - 4));
                }
                g.setFont(g.getFont().deriveFont(Font.BOLD, 11f));
                g.setColor(Color.DARK_GRAY);
                g.drawString("Revenue (RM)", 12, 14);
            } finally {
                g.dispose();
            }
        }
    }
}
