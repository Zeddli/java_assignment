package gui;

import javax.swing.*;
import model.Role;
import model.User;
import service.AccountForm;
import service.StudentService;

/**
 * AD2 Manage Students: add, view, search, update and delete student accounts.
 * Extra feature: "View history" (or double-click a row) shows the student's bookings,
 * payments and attendance totals.
 * INHERITANCE: the table/form/buttons come from AccountPanel; this class fills in the hooks.
 */
public class ManageStudentsPanel extends AccountPanel {
    private final JTextField txtDob = new JTextField();
    private final JButton btnHistory = new JButton("View history");
    private final StudentService students;

    public ManageStudentsPanel() {
        this(new StudentService());
    }

    private ManageStudentsPanel(StudentService students) {
        super("Manage Students", "Add, view, search, update and delete student accounts",
              "e.g. STU001, Nurul or nurul@mail.com", "Student details",
              students,
              "ID", "Name", "Email", "Phone", "Status");
        this.students = students;
        txtDob.setToolTipText("yyyy-MM-dd, e.g. 2004-05-10");
        btnHistory.addActionListener(e -> openHistory());
        buildUi();
    }

    @Override
    protected Object[] rowFor(User u) {
        return new Object[]{u.getId(), u.getFullName(), u.getEmail(), u.getPhone(), u.getStatus()};
    }

    @Override
    protected int[] columnWidths() { return new int[]{55, 100, 130, 85, 65}; }

    @Override
    protected void addExtraFields() { addField("Date of birth (yyyy-MM-dd)", txtDob, AccountForm.EXTRA); }

    @Override
    protected void addExtraButtons(JPanel buttons) { buttons.add(btnHistory); }

    @Override
    protected Role formRole() { return Role.STUDENT; }

    @Override
    protected String formExtra() { return txtDob.getText(); }

    @Override
    protected void fillExtra(User u) { txtDob.setText(u.getExtra()); }

    @Override
    protected void clearExtra() { txtDob.setText(""); }

    @Override
    protected void onSelectionChanged(User selected) { btnHistory.setEnabled(selected != null); }

    private void openHistory() {
        User u = selectedUser();
        if (u != null) new StudentHistoryDialog(SwingUtilities.getWindowAncestor(this), u, students).setVisible(true);
    }

    /** Double-clicking a student row opens the history dialog. */
    @Override
    protected void onRowDoubleClicked(User u) { openHistory(); }
}
