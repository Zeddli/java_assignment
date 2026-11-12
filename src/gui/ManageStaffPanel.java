package gui;

import javax.swing.*;
import model.Role;
import model.User;
import service.AccountForm;
import service.StaffService;

/**
 * AD1 Manage Staff: add, view, search, update and delete Instructors, Facilities Officers and
 * Attendance Officers. The role list never offers Manager, Administrator or Student.
 * Discipline is enabled only for Instructor. Role is locked once the account exists.
 * INHERITANCE: the table/form/buttons come from AccountPanel; this class fills in the hooks.
 */
public class ManageStaffPanel extends AccountPanel {
    private static final String ALL_STAFF = "All staff";

    private final JComboBox<Object> cmbRoleFilter = new JComboBox<>();
    private final JComboBox<Role> cmbRole = new JComboBox<>();
    private final JComboBox<String> cmbDiscipline = new JComboBox<>(StaffService.DISCIPLINES);

    public ManageStaffPanel() {
        super("Manage Staff",
              "Create, view, search, update and delete staff: instructors, facilities officers and attendance officers",
              "e.g. INS001 or Kenji", "Staff details",
              new StaffService(),
              "ID", "Name", "Role", "Username", "Status");

        cmbRoleFilter.addItem(ALL_STAFF);
        for (Role r : service.manageableRoles()) {
            cmbRoleFilter.addItem(r);
            cmbRole.addItem(r);
        }
        ListCellRenderer<Object> roleNames = new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                   boolean sel, boolean focus) {
                Object text = value instanceof Role ? ((Role) value).getDisplayName() : value;
                return super.getListCellRendererComponent(list, text, index, sel, focus);
            }
        };
        cmbRoleFilter.setRenderer(roleNames);
        cmbRole.setRenderer(roleNames);
        cmbRole.addActionListener(e -> cmbDiscipline.setEnabled(cmbRole.getSelectedItem() == Role.INSTRUCTOR));
        cmbRoleFilter.addActionListener(e -> refresh());
        buildUi();
    }

    @Override
    protected Object[] rowFor(User u) {
        return new Object[]{u.getId(), u.getFullName(), u.getRole().getDisplayName(), u.getUsername(), u.getStatus()};
    }

    @Override
    protected int[] columnWidths() { return new int[]{55, 110, 115, 70, 65}; }

    @Override
    protected void addFilterControls(JPanel toolbar) {
        toolbar.add(cmbRoleFilter);
    }

    @Override
    protected Role filterRole() {
        Object r = cmbRoleFilter.getSelectedItem();
        return r instanceof Role ? (Role) r : null;
    }

    @Override
    protected void resetFilters() { cmbRoleFilter.setSelectedIndex(0); }

    @Override
    protected void addTopFields() { addField("Role", cmbRole, AccountForm.ROLE); }

    @Override
    protected void addExtraFields() { addField("Discipline (instructors only)", cmbDiscipline, AccountForm.EXTRA); }

    @Override
    protected Role formRole() { return (Role) cmbRole.getSelectedItem(); }

    @Override
    protected String formExtra() {
        return formRole() == Role.INSTRUCTOR ? (String) cmbDiscipline.getSelectedItem() : "";
    }

    @Override
    protected void fillExtra(User u) {
        cmbRole.setSelectedItem(u.getRole());
        cmbRole.setEnabled(false); // role decides the ID prefix, so it cannot change
        cmbRole.setToolTipText("Role cannot be changed after the account is created");
        if (u.getRole() == Role.INSTRUCTOR) cmbDiscipline.setSelectedItem(u.getExtra());
    }

    @Override
    protected void clearExtra() {
        cmbRole.setEnabled(true);
        cmbRole.setToolTipText(null);
        cmbRole.setSelectedIndex(0);
        cmbDiscipline.setSelectedIndex(0);
        cmbDiscipline.setEnabled(formRole() == Role.INSTRUCTOR);
    }
}
