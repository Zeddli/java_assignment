package gui;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Reusable sidebar component.
 *
 * No hardcoded navigation items.
 * Buttons are added when modules are registered.
 */
public class SidebarPanel extends JPanel {

    private final Map<String, JButton> buttons =
            new LinkedHashMap<>();

    public SidebarPanel() {

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        setPreferredSize(new Dimension(205, 0));
        setBackground(new Color(247, 247, 247));

        setBorder(
                BorderFactory.createEmptyBorder(
                        14, 10, 14, 10));
    }

    /**
     * Adds a navigation button for a registered module.
     */
    public void addModuleButton(
            String name,
            Runnable onClick) {

        if (buttons.containsKey(name)) {
            throw new IllegalArgumentException(
                    "Duplicate sidebar item: " + name);
        }

        JButton button = new JButton(name);

        button.setFont(
                new Font("SansSerif", Font.PLAIN, 12));

        button.setHorizontalAlignment(SwingConstants.LEFT);

        button.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 42));

        button.setFocusPainted(false);
        button.setOpaque(true);

        button.setBackground(Color.WHITE);
        button.setForeground(Color.DARK_GRAY);

        button.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                new Color(210, 210, 210)),
                        BorderFactory.createEmptyBorder(
                                9, 10, 9, 6)));

        // Open the registered module when clicked.
        button.addActionListener(event -> onClick.run());

        buttons.put(name, button);

        add(button);
        add(Box.createVerticalStrut(8));

        revalidate();
        repaint();
    }

    /**
     * Highlights the currently selected module.
     */
    public void setActiveModule(String name) {

        for (Map.Entry<String, JButton> entry
                : buttons.entrySet()) {

            boolean active = entry.getKey().equals(name);

            entry.getValue().setBackground(
                    active
                            ? new Color(69, 82, 103)
                            : Color.WHITE);

            entry.getValue().setForeground(
                    active
                            ? Color.WHITE
                            : Color.DARK_GRAY);
        }
    }
}