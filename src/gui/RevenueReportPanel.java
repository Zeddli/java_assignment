
package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import service.RevenueReportService;
import service.RevenueReportService.RevenueRow;
import util.DataException;

/**
 * MG3 - Revenue Report GUI.
 *
 * Monthly / Yearly revenue and registration reports.
 * Read-only interface.
 */
public class RevenueReportPanel extends JPanel {

    private final RevenueReportService service =
            new RevenueReportService();

    private final JComboBox<String> cmbMode =
            new JComboBox<>(new String[]{
                    "Monthly", "Yearly"
            });

    private final JComboBox<String> cmbYear =
            new JComboBox<>();

    private final JComboBox<String> cmbMonth =
            new JComboBox<>(new String[]{
                    "All months",
                    "January", "February", "March",
                    "April", "May", "June",
                    "July", "August", "September",
                    "October", "November", "December"
            });

    private final StatCard registrationsCard =
            new StatCard("Paid registrations");

    private final StatCard revenueCard =
            new StatCard("Total revenue (RM)");

    private final SortableTable table =
            new SortableTable(
                    "Period",
                    "Registrations",
                    "Revenue (RM)"
            );

    private final JLabel lblMessage = new JLabel(" ");

    public RevenueReportPanel() {

        setLayout(new BorderLayout(10, 10));

        setBorder(BorderFactory.createEmptyBorder(
                12, 14, 12, 14));

        buildInterface();

        loadYears();

        // Disable month filter in yearly mode.
        cmbMode.addActionListener(event ->
                cmbMonth.setEnabled(
                        cmbMode.getSelectedIndex() == 0));

        // Reload when this module is opened.
        addComponentListener(new ComponentAdapter() {

            @Override
            public void componentShown(ComponentEvent event) {
                loadYears();
                refresh();
            }
        });

        refresh();
    }

    /**
     * Build heading, filters, summary cards and table.
     */
    private void buildInterface() {

        JLabel title = new JLabel("Revenue Report");

        title.setFont(
                title.getFont().deriveFont(Font.BOLD, 20f));

        JLabel subtitle = new JLabel(
                "View monthly or yearly class registrations "
                + "and total revenue");

        subtitle.setForeground(Color.DARK_GRAY);

        // Filters.
        JPanel filters =
                new JPanel(new FlowLayout(
                        FlowLayout.LEFT, 8, 0));

        JButton btnApply = new JButton("Apply");
        JButton btnReset = new JButton("Reset");

        filters.add(new JLabel("Report type"));
        filters.add(cmbMode);

        filters.add(new JLabel("Year"));
        filters.add(cmbYear);

        filters.add(new JLabel("Month"));
        filters.add(cmbMonth);

        filters.add(btnApply);
        filters.add(btnReset);

        btnApply.addActionListener(event -> refresh());

        btnReset.addActionListener(event -> resetFilters());

        // Summary cards.
        JPanel summary =
                new JPanel(new GridLayout(1, 2, 8, 0));

        summary.add(registrationsCard);
        summary.add(revenueCard);

        // Header.
        JPanel header = new JPanel();
        header.setLayout(
                new BoxLayout(header, BoxLayout.Y_AXIS));

        title.setAlignmentX(LEFT_ALIGNMENT);
        subtitle.setAlignmentX(LEFT_ALIGNMENT);
        filters.setAlignmentX(LEFT_ALIGNMENT);
        summary.setAlignmentX(LEFT_ALIGNMENT);

        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitle);
        header.add(Box.createVerticalStrut(12));
        header.add(filters);
        header.add(Box.createVerticalStrut(12));
        header.add(summary);

        add(header, BorderLayout.NORTH);

        // Revenue table.
        table.setColumnWidths(160, 130, 160);

        add(new JScrollPane(table), BorderLayout.CENTER);

        lblMessage.setForeground(Color.DARK_GRAY);

        add(lblMessage, BorderLayout.SOUTH);
    }

    /**
     * Load available years without losing the current
     * year selection when records are refreshed.
     */
    private void loadYears() {

        String previous =
                (String) cmbYear.getSelectedItem();

        cmbYear.removeAllItems();
        cmbYear.addItem("All years");

        for (Integer year : service.availableYears()) {
            cmbYear.addItem(String.valueOf(year));
        }

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

    /**
     * Apply filters and update the report.
     */
    private void refresh() {

        boolean monthly =
                cmbMode.getSelectedIndex() == 0;

        Integer year = null;
        Integer month = null;

        String selectedYear =
                (String) cmbYear.getSelectedItem();

        if (selectedYear != null
                && !"All years".equals(selectedYear)) {

            year = Integer.parseInt(selectedYear);
        }

        if (monthly && cmbMonth.getSelectedIndex() > 0) {
            month = cmbMonth.getSelectedIndex();
        }

        try {

            List<RevenueRow> rows =
                    service.generateReport(
                            monthly, year, month);

            table.clearRows();

            int totalRegistrations = 0;
            double totalRevenue = 0;

            for (RevenueRow row : rows) {

                table.addRow(
                        row.getPeriod(),
                        row.getRegistrations(),
                        row.getRevenue()
                );

                totalRegistrations +=
                        row.getRegistrations();

                totalRevenue += row.getRevenue();
            }

            registrationsCard.setValue(
                    String.valueOf(totalRegistrations));

            revenueCard.setValue(
                    String.format("RM %,.2f", totalRevenue));

            lblMessage.setForeground(Color.DARK_GRAY);

            lblMessage.setText(
                    rows.isEmpty()
                            ? "No revenue records found."
                            : rows.size() + " reporting period(s) shown");

        } catch (DataException ex) {

            table.clearRows();

            registrationsCard.setValue("0");
            revenueCard.setValue("RM 0.00");

            lblMessage.setForeground(
                    new Color(185, 0, 0));

            lblMessage.setText(
                    "Unable to load revenue: "
                    + ex.getMessage());
        }
    }

    /**
     * Restore the unfiltered monthly report.
     */
    private void resetFilters() {

        cmbMode.setSelectedIndex(0);
        cmbYear.setSelectedIndex(0);
        cmbMonth.setSelectedIndex(0);

        refresh();
    }
}
