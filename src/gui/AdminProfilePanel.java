package gui;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.Border;
import model.User;
import service.AccountForm;
import service.ProfileService;
import util.DataException;
import util.ValidationException;

/**
 * Personalisation feature: "My Profile" for the logged-in administrator.
 * Left card: account summary (read-only) and editable name, email and phone.
 * Right card: change password (current + new + confirm, with a show-password option).
 * All rules live in ProfileService; this screen only shows fields and messages.
 */
public class AdminProfilePanel extends JPanel {
    private final String adminId;
    private final ProfileService service = new ProfileService();
    private final WelcomeHeader welcome;

    // account summary (read-only)
    private final JLabel lblId = value();
    private final JLabel lblRole = value();
    private final JLabel lblUsername = value();
    private final JLabel lblStatus = value();

    // personal details
    private final JTextField txtName = new JTextField();
    private final JTextField txtEmail = new JTextField();
    private final JTextField txtPhone = new JTextField();
    private final JLabel lblDetailsMsg = message();

    // password
    private final JPasswordField txtCurrent = new JPasswordField();
    private final JPasswordField txtNew = new JPasswordField();
    private final JPasswordField txtConfirm = new JPasswordField();
    private final JCheckBox chkShow = new JCheckBox("Show passwords");
    private final JLabel lblPasswordMsg = message();

    /** Field key -> input box, so a ValidationException can highlight the right one. */
    private final Map<String, JComponent> fields = new HashMap<>();
    private final Map<JComponent, Border> normalBorders = new HashMap<>();
    private final char echo = txtCurrent.getEchoChar();

    public AdminProfilePanel(String adminId) {
        this.adminId = adminId;
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        welcome = new WelcomeHeader(adminId, "My Profile",
                "View your account and update your personal details or password");
        add(welcome, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(1, 2, 14, 0));
        cards.add(topAligned(buildDetailsCard()));
        cards.add(topAligned(buildPasswordCard()));
        add(cards, BorderLayout.CENTER);

        register(AccountForm.FULL_NAME, txtName);
        register(AccountForm.EMAIL, txtEmail);
        register(AccountForm.PHONE, txtPhone);
        register(ProfileService.CURRENT, txtCurrent);
        register(ProfileService.NEW, txtNew);
        register(ProfileService.CONFIRM, txtConfirm);

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) { welcome.refresh(); load(); }
        });
        load();
    }

    // =================== layout ===================

    private JPanel buildDetailsCard() {
        FormCard card = new FormCard("Personal details");
        card.addInfoRow("User ID", lblId);
        card.addInfoRow("Role", lblRole);
        card.addInfoRow("Username", lblUsername);
        card.addInfoRow("Status", lblStatus);
        card.addGap(6);
        card.addField("Full name", txtName);
        card.addField("Email", txtEmail);
        card.addField("Phone", txtPhone);
        card.addRow(lblDetailsMsg);

        JButton btnSave = new AdminButton("Save changes", AdminButton.Style.PRIMARY);
        JButton btnUndo = new AdminButton("Undo changes");
        btnSave.addActionListener(e -> saveDetails());
        btnUndo.addActionListener(e -> { load(); lblDetailsMsg.setText(" "); });
        card.addButtons(btnSave, btnUndo);
        return card;
    }

    private JPanel buildPasswordCard() {
        FormCard card = new FormCard("Change password");
        JLabel hint = AdminTheme.muted("At least 6 characters, no spaces.");
        hint.setFont(hint.getFont().deriveFont(11.5f));
        card.addRow(hint);
        card.addField("Current password", txtCurrent);
        card.addField("New password", txtNew);
        card.addField("Confirm new password", txtConfirm);
        chkShow.setOpaque(false);
        chkShow.addActionListener(e -> {
            char c = chkShow.isSelected() ? (char) 0 : echo; // 0 = show the characters
            txtCurrent.setEchoChar(c);
            txtNew.setEchoChar(c);
            txtConfirm.setEchoChar(c);
        });
        card.addRow(chkShow);
        card.addRow(lblPasswordMsg);

        JButton btnChange = new AdminButton("Change password", AdminButton.Style.PRIMARY);
        JButton btnClear = new AdminButton("Clear");
        btnChange.addActionListener(e -> changePassword());
        btnClear.addActionListener(e -> { clearPasswords(); lblPasswordMsg.setText(" "); });
        card.addButtons(btnChange, btnClear);
        return card;
    }

    // =================== actions ===================

    private void load() {
        clearHighlights();
        try {
            User me = service.load(adminId);
            lblId.setText(me.getId());
            lblRole.setText(me.getRole().getDisplayName());
            lblUsername.setText(me.getUsername());
            lblStatus.setText(me.getStatus());
            lblStatus.setForeground(me.isActive() ? AdminTheme.SUCCESS : AdminTheme.NEUTRAL);
            txtName.setText(me.getFullName());
            txtEmail.setText(me.getEmail());
            txtPhone.setText(me.getPhone());
        } catch (ValidationException | DataException ex) {
            error(lblDetailsMsg, null, ex.getMessage());
        }
    }

    private void saveDetails() {
        clearHighlights();
        try {
            service.updateDetails(adminId, txtName.getText(), txtEmail.getText(), txtPhone.getText());
            load();
            welcome.refresh(); // the greeting uses the new name straight away
            success(lblDetailsMsg, "Saved. The name in the top bar updates the next time you log in.");
        } catch (ValidationException ex) {
            error(lblDetailsMsg, ex.getField(), ex.getMessage());
        } catch (DataException ex) {
            error(lblDetailsMsg, null, "Could not save: " + ex.getMessage());
        }
    }

    private void changePassword() {
        clearHighlights();
        try {
            service.changePassword(adminId, new String(txtCurrent.getPassword()),
                    new String(txtNew.getPassword()), new String(txtConfirm.getPassword()));
            clearPasswords();
            success(lblPasswordMsg, "Password changed. Use it the next time you log in.");
        } catch (ValidationException ex) {
            error(lblPasswordMsg, ex.getField(), ex.getMessage());
        } catch (DataException ex) {
            error(lblPasswordMsg, null, "Could not save: " + ex.getMessage());
        }
    }

    private void clearPasswords() {
        txtCurrent.setText("");
        txtNew.setText("");
        txtConfirm.setText("");
    }

    // =================== messages ===================

    private void register(String key, JComponent field) {
        fields.put(key, field);
        normalBorders.put(field, field.getBorder());
    }

    private void error(JLabel target, String fieldKey, String text) {
        target.setForeground(AdminTheme.ERROR);
        target.setText("<html>" + escape(text) + "</html>");
        JComponent f = fieldKey == null ? null : fields.get(fieldKey);
        if (f != null) {
            f.setBorder(BorderFactory.createLineBorder(AdminTheme.ERROR, 2));
            f.requestFocusInWindow();
        }
    }

    private static void success(JLabel target, String text) {
        target.setForeground(AdminTheme.SUCCESS);
        target.setText("<html>" + escape(text) + "</html>");
    }

    private void clearHighlights() {
        for (Map.Entry<JComponent, Border> e : normalBorders.entrySet()) e.getKey().setBorder(e.getValue());
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static JLabel value() {
        JLabel l = new JLabel("-");
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        l.setForeground(AdminTheme.TEXT);
        return l;
    }

    private static JLabel message() {
        JLabel l = new JLabel(" ");
        l.setFont(l.getFont().deriveFont(11.5f));
        return l;
    }

    private static JPanel topAligned(JComponent c) {
        JPanel p = new JPanel(new BorderLayout());
        p.add(c, BorderLayout.NORTH);
        return p;
    }

    /** White card with a title and stacked label/field rows (one small reusable layout helper). */
    private static class FormCard extends JPanel {
        private final GridBagConstraints c = new GridBagConstraints();

        FormCard(String title) {
            super(new GridBagLayout());
            setBackground(Color.WHITE);
            setBorder(AdminTheme.cardBorder());
            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(0, 0, 6, 0);
            add(AdminTheme.sectionTitle(title), c);
        }

        void addRow(JComponent comp) {
            c.gridy++;
            c.insets = new Insets(2, 0, 2, 0);
            add(comp, c);
        }

        void addGap(int px) { addRow((JComponent) Box.createVerticalStrut(px)); }

        void addField(String label, JComponent field) {
            JLabel l = new JLabel(label);
            l.setForeground(AdminTheme.TEXT);
            c.gridy++;
            c.insets = new Insets(4, 0, 0, 0);
            add(l, c);
            c.gridy++;
            c.insets = new Insets(1, 0, 0, 0);
            add(field, c);
        }

        void addInfoRow(String label, JLabel value) {
            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(false);
            JLabel l = AdminTheme.muted(label);
            l.setPreferredSize(new Dimension(90, l.getPreferredSize().height));
            row.add(l, BorderLayout.WEST);
            row.add(value, BorderLayout.CENTER);
            addRow(row);
        }

        void addButtons(JButton... buttons) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            row.setOpaque(false);
            for (int i = 0; i < buttons.length; i++) {
                if (i > 0) row.add(Box.createHorizontalStrut(6));
                row.add(buttons[i]);
            }
            c.gridy++;
            c.insets = new Insets(8, 0, 0, 0);
            add(row, c);
        }
    }
}
