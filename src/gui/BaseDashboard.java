package gui;

import java.awt.*;
import javax.swing.*;
import model.User;
 
/**
 * Shared window for ALL roles (960 x 600): dark top bar, left menu, CardLayout content.
 * A role dashboard only extends this and calls addModule(...) three times.
 */
public abstract class BaseDashboard extends JFrame {
    protected final User user;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JPanel menu = new JPanel(new GridLayout(0, 1, 0, 8));
 
    protected BaseDashboard(User user, String roleLabel, Color accent) {
        super("SDAMS - " + roleLabel);
        this.user = user;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(960, 600);
        setLocationRelativeTo(null);
 
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(new Color(26, 26, 26));
        top.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        JLabel title = new JLabel("SDAMS \u00b7 Self Defence Academy");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));
        JLabel badge = new JLabel(" " + roleLabel + " ");
        badge.setOpaque(true);
        badge.setBackground(accent);
        badge.setForeground(Color.WHITE);
        JLabel name = new JLabel(user.getFullName());
        name.setForeground(Color.WHITE);
        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> {
            new LoginFrame().setVisible(true);
            dispose();
        });
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(badge); right.add(name); right.add(logout);
        top.add(title, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);
 
        menu.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
        JPanel left = new JPanel(new BorderLayout());
        left.setPreferredSize(new Dimension(190, 0));
        left.add(menu, BorderLayout.NORTH);
 
        add(top, BorderLayout.NORTH);
        add(left, BorderLayout.WEST);
        add(content, BorderLayout.CENTER);
    }
 
    protected final void addModule(String menuTitle, JPanel panel) {
        content.add(panel, menuTitle);
        JButton b = new JButton(menuTitle);
        b.addActionListener(e -> cards.show(content, menuTitle));
        menu.add(b);
    }
}
