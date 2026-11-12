package gui;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.Border;
import service.PaymentEntry;
import service.PaymentService;
import util.DataException;
import util.ValidationException;

/**
 * AD3 Payment Records: view and filter past payments made by students (read-only).
 * Filters: student ID or name, from date, to date, method. Filters combine (AND); blank ones are ignored.
 * Validation: dates must be yyyy-MM-dd and from-date cannot be after to-date. No add/edit/delete here.
 */
public class PaymentRecordsPanel extends JPanel {
    private static final Color ERROR = new Color(185, 0, 0);

    private final PaymentService service = new PaymentService();
    private final JTextField txtStudent = new HintTextField("e.g. STU001", 10);
    private final JTextField txtFrom = new HintTextField("yyyy-MM-dd", 8);
    private final JTextField txtTo = new HintTextField("yyyy-MM-dd", 8);
    private final JComboBox<String> cmbMethod = new JComboBox<>();
    private final JLabel lblError = new JLabel(" ");
    private final JLabel lblCount = new JLabel(" ");
    private final JLabel lblTotal = new JLabel("RM 0.00");
    private final SortableTable table =
            new SortableTable("Payment ID", "Student", "Class", "Method", "Paid on", "Amount (RM)");
    private final Border normalBorder = txtFrom.getBorder();

    public PaymentRecordsPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JLabel heading = new JLabel("Payment Records");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 20f));
        JLabel sub = new JLabel("View and filter past payments made by students (read-only)");
        sub.setForeground(Color.DARK_GRAY);

        cmbMethod.addItem("All");
        for (String m : PaymentService.METHODS) cmbMethod.addItem(m);
        txtStudent.setToolTipText("e.g. STU001 or Nurul");
        txtFrom.setToolTipText("yyyy-MM-dd");
        txtTo.setToolTipText("yyyy-MM-dd");

        JButton btnFilter = new JButton("Filter");
        btnFilter.setFont(btnFilter.getFont().deriveFont(Font.BOLD));
        JButton btnReset = new JButton("Reset");
        btnFilter.addActionListener(e -> applyFilter());
        btnReset.addActionListener(e -> reset());
        for (JTextField f : new JTextField[]{txtStudent, txtFrom, txtTo}) f.addActionListener(e -> applyFilter());

        JPanel filters = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 2, 6);
        c.anchor = GridBagConstraints.WEST;
        String[] labels = {"Student ID or name", "From date", "To date", "Method"};
        JComponent[] inputs = {txtStudent, txtFrom, txtTo, cmbMethod};
        for (int i = 0; i < labels.length; i++) {
            c.gridx = i; c.gridy = 0; filters.add(new JLabel(labels[i]), c);
            c.gridy = 1; c.fill = GridBagConstraints.HORIZONTAL; filters.add(inputs[i], c);
            c.fill = GridBagConstraints.NONE;
        }
        c.gridy = 1; c.gridx = 4; filters.add(btnFilter, c);
        c.gridx = 5; filters.add(btnReset, c);

        lblError.setForeground(ERROR);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        for (JComponent comp : new JComponent[]{heading, sub, (JComponent) Box.createVerticalStrut(8), filters, lblError}) {
            comp.setAlignmentX(LEFT_ALIGNMENT);
            header.add(comp);
        }

        lblTotal.setFont(lblTotal.getFont().deriveFont(Font.BOLD, 14f));
        JPanel totalBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        totalBox.add(new JLabel("Total of rows shown:"));
        totalBox.add(lblTotal);
        JPanel footer = new JPanel(new BorderLayout());
        lblCount.setForeground(Color.DARK_GRAY);
        footer.add(lblCount, BorderLayout.WEST);
        footer.add(totalBox, BorderLayout.EAST);

        table.setColumnWidths(80, 150, 130, 110, 90, 90);
        add(header, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        // refresh every time the screen is opened, so new student payments appear
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) { applyFilter(); }
        });
        applyFilter();
    }

    private void applyFilter() {
        lblError.setText(" ");
        txtFrom.setBorder(normalBorder);
        txtTo.setBorder(normalBorder);
        try {
            List<PaymentEntry> rows = service.filter(txtStudent.getText(), txtFrom.getText(),
                    txtTo.getText(), (String) cmbMethod.getSelectedItem());
            table.clearRows();
            for (PaymentEntry p : rows) {
                table.addRow(p.getPaymentId(), p.getStudentLabel(), p.getClassLabel(),
                        p.getMethod(), p.getPaidDate(), p.getAmount());
            }
            lblCount.setText(rows.isEmpty() ? "No records found"
                    : rows.size() + " payment" + (rows.size() == 1 ? "" : "s") + " shown");
            lblTotal.setText(String.format("RM %,.2f", PaymentService.total(rows)));
        } catch (ValidationException ex) {
            lblError.setText(ex.getMessage());
            JTextField bad = "to".equals(ex.getField()) ? txtTo : txtFrom;
            bad.setBorder(BorderFactory.createLineBorder(ERROR, 2));
            bad.requestFocusInWindow();
        } catch (DataException ex) {
            lblError.setText("Could not read the data files: " + ex.getMessage());
        }
    }

    private void reset() {
        txtStudent.setText("");
        txtFrom.setText("");
        txtTo.setText("");
        cmbMethod.setSelectedIndex(0);
        applyFilter();
    }
}
