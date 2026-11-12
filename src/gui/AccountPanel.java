package gui;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.Border;
import model.Role;
import model.User;
import service.AccountForm;
import service.AccountService;
import util.DataException;
import util.ValidationException;

/**
 * Shared layout of AD1 and AD2 (wireframe rule): toolbar (filter/search) -> JTable -> side form
 * with Add, Update, Delete, Clear. Clicking a row fills the form; Update/Delete stay disabled
 * until a row is selected. Errors show inline in red; Delete and Deactivate ask for confirmation.
 *
 * ABSTRACTION: subclasses only describe their columns, their role-specific form fields and how
 * to read/fill them. Everything else (events, validation display, confirm dialogs) lives here once.
 * POLYMORPHISM: {@code service} is a StaffService or StudentService behind an AccountService type.
 */
public abstract class AccountPanel extends JPanel {
    protected static final Color ERROR = new Color(185, 0, 0);
    protected static final Color PRIMARY = new Color(52, 94, 210);
    private static final Border ERROR_BORDER = BorderFactory.createLineBorder(ERROR, 2);

    protected final AccountService service;

    private final SortableTable table;
    private final JTextField txtSearch = new HintTextField("Search ID, name or email", 13);
    private final JComboBox<String> cmbStatus = new JComboBox<>(new String[]{"All statuses", "Active", "Inactive"});
    private final JLabel lblCount = new JLabel(" ");

    private final JPanel form = new FormPanel();
    private int formRow = 0;
    protected final JTextField txtFullName = new JTextField();
    protected final JTextField txtUsername = new JTextField();
    private String passwordHint = "";
    protected final JPasswordField txtPassword = new JPasswordField() {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            HintTextField.paintHint(this, g, passwordHint);
        }
    };
    protected final JTextField txtEmail = new JTextField();
    protected final JTextField txtPhone = new JTextField();
    private final JLabel lblFormTitle = new JLabel();
    private final JLabel lblError = new JLabel(" ");

    private final JButton btnAdd = new JButton("Add");
    private final JButton btnUpdate = new JButton("Update");
    private final JButton btnDelete = new JButton("Delete");
    private final JButton btnStatus = new JButton("Deactivate");
    private final JButton btnClear = new JButton("Clear");

    /** Field key (AccountForm.*) -> input box, so errors can highlight the right box. */
    private final Map<String, JComponent> fieldsByKey = new LinkedHashMap<>();
    private final Map<JComponent, Border> originalBorders = new LinkedHashMap<>();

    private String selectedId; // null = nothing selected (Add mode)
    private final String title, subtitle, searchHint, formTitle;

    protected AccountPanel(String title, String subtitle, String searchHint, String formTitle,
                           AccountService service, String... columns) {
        this.service = service;
        this.table = new SortableTable(columns);
        this.title = title;
        this.subtitle = subtitle;
        this.searchHint = searchHint;
        this.formTitle = formTitle;
    }

    /**
     * Builds the screen. Subclasses call this LAST in their constructor, because the hooks
     * (addTopFields, addExtraFields...) use the subclass's own fields, which Java only
     * initialises after this parent constructor has finished.
     */
    protected final void buildUi() {
        setLayout(new BorderLayout(10, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        if (columnWidths() != null) table.setColumnWidths(columnWidths());
        add(buildHeader(title, subtitle, searchHint), BorderLayout.NORTH);
        add(buildTableArea(), BorderLayout.CENTER);
        add(buildForm(formTitle), BorderLayout.EAST);
        wireEvents();
        clearForm();
        refresh();
        // reload whenever the screen is opened, so changes made elsewhere show up
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) { refresh(); }
        });
    }

    // =================== hooks for subclasses ===================

    /** One table row for this account. Column 0 must be the user ID. */
    protected abstract Object[] rowFor(User u);

    /** Adds fields that appear ABOVE full name (e.g. the staff Role list). Default: none. */
    protected void addTopFields() { }

    /** Adds the role-specific field BELOW phone (Discipline or Date of birth). */
    protected abstract void addExtraFields();

    /** Role chosen in the form. */
    protected abstract Role formRole();

    /** Value of the role-specific field. */
    protected abstract String formExtra();

    /** Fills the role-specific fields from a selected account (and locks what cannot change). */
    protected abstract void fillExtra(User u);

    /** Resets the role-specific fields for a new account. */
    protected abstract void clearExtra();

    /** Adds extra controls to the toolbar before the search box (e.g. a role filter). Default: none. */
    protected void addFilterControls(JPanel toolbar) { }

    /** Role filter chosen in the toolbar (null = all). Default: all. */
    protected Role filterRole() { return null; }

    /** Preferred column widths for the table (null = equal widths). */
    protected int[] columnWidths() { return null; }

    /** Adds extra buttons under the form (e.g. View history). Default: none. */
    protected void addExtraButtons(JPanel buttons) { }

    /** Called whenever the selected row changes (null = nothing selected). */
    protected void onSelectionChanged(User selected) { }

    /** Called when a row is double-clicked. Default: nothing. */
    protected void onRowDoubleClicked(User u) { }

    // =================== helpers for subclasses ===================

    /** Adds "label + component" to the side form and remembers it for error highlighting. */
    protected final void addField(String label, JComponent input, String fieldKey) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridy = formRow++;
        c.insets = new Insets(3, 0, 0, 0);
        form.add(new JLabel(label), c);
        c.gridy = formRow++;
        c.insets = new Insets(0, 0, 0, 0);
        form.add(input, c);
        if (fieldKey != null) {
            fieldsByKey.put(fieldKey, input);
            originalBorders.put(input, input.getBorder());
        }
    }

    /** Adds any component as a full-width row of the side form. */
    protected final void addFormRow(JComponent comp) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = formRow++;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        form.add(comp, c);
    }

    protected final User selectedUser() { return selectedId == null ? null : service.find(selectedId); }

    /** Reloads the table with the current filters. */
    protected final void refresh() {
        String status = null;
        if (cmbStatus.getSelectedIndex() == 1) status = AccountService.ACTIVE;
        if (cmbStatus.getSelectedIndex() == 2) status = AccountService.INACTIVE;
        try {
            List<User> users = service.search(txtSearch.getText(), filterRole(), status);
            table.clearRows();
            int active = 0;
            for (User u : users) {
                table.addRow(rowFor(u));
                if (u.isActive()) active++;
            }
            lblCount.setText(users.isEmpty()
                    ? "No records found"
                    : users.size() + " record" + (users.size() == 1 ? "" : "s") + " shown  ·  "
                      + active + " active  ·  " + (users.size() - active) + " inactive");
            reselect();
        } catch (DataException ex) {
            showError(null, "Could not read the data file: " + ex.getMessage());
        }
    }

    // =================== layout ===================

    private JPanel buildHeader(String title, String subtitle, String searchHint) {
        JLabel heading = new JLabel(title);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 20f));
        JLabel sub = new JLabel(subtitle);
        sub.setForeground(Color.DARK_GRAY);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        addFilterControls(toolbar);
        toolbar.add(cmbStatus);
        txtSearch.setToolTipText("Search by ID, name, username or email, " + searchHint);
        toolbar.add(txtSearch);
        JButton btnSearch = new JButton("Search");
        JButton btnShowAll = new JButton("Show all");
        btnSearch.addActionListener(e -> refresh());
        btnShowAll.addActionListener(e -> showAll());
        txtSearch.addActionListener(e -> refresh()); // Enter key searches too
        toolbar.add(btnSearch);
        toolbar.add(btnShowAll);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        for (JComponent c : new JComponent[]{heading, sub, (JComponent) Box.createVerticalStrut(8), toolbar}) {
            c.setAlignmentX(LEFT_ALIGNMENT);
            header.add(c);
        }
        return header;
    }

    private JPanel buildTableArea() {
        JPanel area = new JPanel(new BorderLayout(0, 4));
        area.add(new JScrollPane(table), BorderLayout.CENTER);
        lblCount.setForeground(Color.DARK_GRAY);
        area.add(lblCount, BorderLayout.SOUTH);
        return area;
    }

    private JComponent buildForm(String formTitle) {
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        lblFormTitle.setText(formTitle);
        lblFormTitle.setFont(lblFormTitle.getFont().deriveFont(Font.BOLD, 14f));
        addFormRow(lblFormTitle);
        addTopFields();
        addField("Full name", txtFullName, AccountForm.FULL_NAME);
        addField("Username", txtUsername, AccountForm.USERNAME);
        addField("Password", txtPassword, AccountForm.PASSWORD);
        addField("Email", txtEmail, AccountForm.EMAIL);
        addField("Phone", txtPhone, AccountForm.PHONE);
        addExtraFields();

        lblError.setForeground(ERROR);
        lblError.setFont(lblError.getFont().deriveFont(11f));
        lblError.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        addFormRow(lblError);

        btnAdd.setFont(btnAdd.getFont().deriveFont(Font.BOLD));
        btnDelete.setForeground(ERROR);
        JPanel row1 = new JPanel(new GridLayout(1, 3, 2, 0));
        row1.add(btnAdd); row1.add(btnUpdate); row1.add(btnDelete);
        JPanel row2 = new JPanel(new GridLayout(1, 0, 2, 0));
        row2.add(btnStatus); row2.add(btnClear);
        JPanel row3 = new JPanel(new GridLayout(1, 0, 2, 0));
        addExtraButtons(row3);
        JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 2));
        buttons.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        buttons.add(row1); buttons.add(row2);
        if (row3.getComponentCount() > 0) buttons.add(row3);
        addFormRow(buttons);

        // push everything to the top of the form
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridy = formRow++;
        filler.weighty = 1;
        form.add(Box.createGlue(), filler);

        JScrollPane scroll = new JScrollPane(form, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        scroll.setPreferredSize(new Dimension(290, 0));
        return scroll;
    }

    /** Side form that always matches the scroll pane's width (only scrolls vertically). */
    private static class FormPanel extends JPanel implements Scrollable {
        FormPanel() { super(new GridBagLayout()); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 12; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() {
            return getParent() != null && getParent().getHeight() > getPreferredSize().height;
        }
    }

    // =================== events ===================

    private void wireEvents() {
        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            Object id = table.selectedValue(0);
            if (id == null) {
                if (selectedId != null) clearForm();
            } else if (!id.equals(selectedId)) {
                fillForm(service.find((String) id));
            }
        });
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                User u = selectedUser();
                if (e.getClickCount() == 2 && u != null) onRowDoubleClicked(u);
            }
        });
        cmbStatus.addActionListener(e -> refresh());
        btnAdd.addActionListener(e -> doAdd());
        btnUpdate.addActionListener(e -> doUpdate());
        btnDelete.addActionListener(e -> doDelete());
        btnStatus.addActionListener(e -> doToggleStatus());
        btnClear.addActionListener(e -> { table.clearSelection(); clearForm(); });
    }

    private void showAll() {
        txtSearch.setText("");
        cmbStatus.setSelectedIndex(0); // fires refresh()
        resetFilters();
        refresh();
    }

    /** Subclasses with extra filters reset them here. */
    protected void resetFilters() { }

    private AccountForm readForm() {
        return new AccountForm(formRole(), txtFullName.getText(), txtUsername.getText(),
                new String(txtPassword.getPassword()), txtEmail.getText(), txtPhone.getText(), formExtra());
    }

    private void doAdd() {
        clearErrors();
        try {
            User created = service.add(readForm());
            selectedId = null;
            refresh();
            selectRow(created.getId());
            info(created.getRole().getDisplayName() + " " + created.getId() + " added.");
        } catch (ValidationException ex) {
            showError(ex.getField(), ex.getMessage());
        } catch (DataException ex) {
            showError(null, "Could not save: " + ex.getMessage());
        }
    }

    private void doUpdate() {
        if (selectedId == null) return;
        clearErrors();
        try {
            User updated = service.update(selectedId, readForm());
            refresh();
            fillForm(updated);
            info(updated.getId() + " updated.");
        } catch (ValidationException ex) {
            showError(ex.getField(), ex.getMessage());
        } catch (DataException ex) {
            showError(null, "Could not save: " + ex.getMessage());
        }
    }

    /** Hybrid delete: hard delete when nothing refers to the account, otherwise offer INACTIVE. */
    private void doDelete() {
        User u = selectedUser();
        if (u == null) return;
        clearErrors();
        String history = service.findHistory(u);
        try {
            if (history != null) {
                if (!u.isActive()) {
                    Dialogs.inform(this, u.getId() + " has " + history
                            + " so it cannot be deleted.\nIt is already INACTIVE and cannot log in.",
                            "Cannot delete", Dialogs.Kind.INFO);
                    return;
                }
                boolean deactivate = Dialogs.confirm(this,
                        u.getFullName() + " (" + u.getId() + ") has " + history + ".\n"
                        + "Deleting would break that history, so the account cannot be removed.\n\n"
                        + "Mark the account INACTIVE instead? (They will no longer be able to log in.)",
                        "Cannot delete", Dialogs.Kind.WARNING);
                if (deactivate) {
                    service.setStatus(u.getId(), AccountService.INACTIVE);
                    refresh();
                    info(u.getId() + " marked INACTIVE.");
                }
                return;
            }
            if (!Dialogs.confirm(this,
                    "Permanently delete " + u.getFullName() + " (" + u.getId() + ")?\nThis cannot be undone.",
                    "Confirm delete", Dialogs.Kind.WARNING)) return;
            service.delete(u.getId());
            table.clearSelection();
            clearForm();
            refresh();
            info(u.getId() + " deleted.");
        } catch (ValidationException ex) {
            showError(ex.getField(), ex.getMessage());
        } catch (DataException ex) {
            showError(null, "Could not save: " + ex.getMessage());
        }
    }

    /** Extra feature: switch ACTIVE <-> INACTIVE without deleting. */
    private void doToggleStatus() {
        User u = selectedUser();
        if (u == null) return;
        clearErrors();
        boolean deactivate = u.isActive();
        String msg;
        if (deactivate) {
            String warning = service.deactivationWarning(u);
            msg = "Deactivate " + u.getFullName() + " (" + u.getId() + ")?\nThey will not be able to log in."
                    + (warning == null ? "" : "\n\nNote: " + warning);
        } else {
            msg = "Activate " + u.getFullName() + " (" + u.getId() + ")?\nThey will be able to log in again.";
        }
        if (!Dialogs.confirm(this, msg, deactivate ? "Confirm deactivate" : "Confirm activate",
                deactivate ? Dialogs.Kind.WARNING : Dialogs.Kind.QUESTION)) return;
        try {
            User changed = service.setStatus(u.getId(), deactivate ? AccountService.INACTIVE : AccountService.ACTIVE);
            refresh();
            fillForm(changed);
            info(changed.getId() + " is now " + changed.getStatus() + ".");
        } catch (ValidationException ex) {
            showError(ex.getField(), ex.getMessage());
        } catch (DataException ex) {
            showError(null, "Could not save: " + ex.getMessage());
        }
    }

    // =================== form state ===================

    private void fillForm(User u) {
        clearErrors();
        if (u == null) { clearForm(); return; }
        selectedId = u.getId();
        lblFormTitle.setText("Edit " + u.getId());
        txtFullName.setText(u.getFullName());
        txtUsername.setText(u.getUsername());
        txtPassword.setText("");
        passwordHint = "Leave blank to keep current";
        txtPassword.repaint();
        txtEmail.setText(u.getEmail());
        txtPhone.setText(u.getPhone());
        fillExtra(u);
        btnAdd.setEnabled(false);
        btnUpdate.setEnabled(true);
        btnDelete.setEnabled(true);
        btnStatus.setEnabled(true);
        btnStatus.setText(u.isActive() ? "Deactivate" : "Activate");
        onSelectionChanged(u);
    }

    private void clearForm() {
        clearErrors();
        selectedId = null;
        lblFormTitle.setText("New account");
        for (JTextField f : new JTextField[]{txtFullName, txtUsername, txtPassword, txtEmail, txtPhone}) f.setText("");
        passwordHint = "At least 6 characters";
        txtPassword.repaint();
        clearExtra();
        btnAdd.setEnabled(true);
        btnUpdate.setEnabled(false);
        btnDelete.setEnabled(false);
        btnStatus.setEnabled(false);
        btnStatus.setText("Deactivate");
        onSelectionChanged(null);
    }

    /** After a refresh, keep the same account selected if it is still in the table. */
    private void reselect() {
        if (selectedId == null) return;
        String keep = selectedId;
        if (!selectRow(keep)) clearForm();
    }

    private boolean selectRow(String id) {
        for (int view = 0; view < table.getRowCount(); view++) {
            if (id.equals(table.getValueAt(view, 0))) {
                table.setRowSelectionInterval(view, view);
                table.scrollRectToVisible(table.getCellRect(view, 0, true));
                return true;
            }
        }
        return false;
    }

    // =================== messages ===================

    private void showError(String fieldKey, String message) {
        lblError.setForeground(ERROR);
        lblError.setText("<html>" + escape(message) + "</html>");
        JComponent field = fieldKey == null ? null : fieldsByKey.get(fieldKey);
        if (field != null) {
            field.setBorder(ERROR_BORDER);
            field.requestFocusInWindow();
        }
    }

    private void info(String message) {
        lblError.setForeground(new Color(0, 120, 60));
        lblError.setText("<html>" + escape(message) + "</html>");
    }

    private void clearErrors() {
        lblError.setText(" ");
        for (JComponent c : fieldsByKey.values()) c.setBorder(originalBorders.get(c));
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
