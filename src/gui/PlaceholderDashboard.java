package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.*;
import model.User;
 
/** Shown for roles whose owner has not plugged their screens in yet. */
public class PlaceholderDashboard extends BaseDashboard {
    public PlaceholderDashboard(User user) {
        super(user, user.getRole().getDisplayName(), new Color(90, 90, 90));
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JLabel("Screens for " + user.getRole().getDisplayName() + " are not built yet.", SwingConstants.CENTER));
        addModule("Coming soon", p);
    }
}

