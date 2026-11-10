package gui;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import model.User;

/**
 * Shared dashboard frame.
 *
 * Responsibilities:
 * 1. Display shared top bar.
 * 2. Display reusable sidebar.
 * 3. Allow modules to register their JPanel.
 * 4. Switch modules using CardLayout.
 *
 * No role-specific functionality is implemented here.
 */
public class DashboardFrame extends JFrame {

    private final CardLayout cardLayout =
            new CardLayout();

    private final JPanel contentArea =
            new JPanel(cardLayout);

    private final SidebarPanel sidebar =
            new SidebarPanel();

    private final Map<String, JPanel> modules =
            new LinkedHashMap<>();

    public DashboardFrame(
            User user,
            Runnable openLogin) {

        setTitle("SDAMS - "
                + user.getRole().getDisplayName());

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setSize(960, 600);
        setMinimumSize(new Dimension(760, 500));

        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // ===========================
        // Shared top bar
        // ===========================

        JPanel topBar = new JPanel(new BorderLayout());

        topBar.setBackground(new Color(30, 30, 30));

        topBar.setBorder(
                BorderFactory.createEmptyBorder(
                        7, 16, 7, 16));

        JLabel appName =
                new JLabel("SDAMS - Self Defence Academy");

        appName.setForeground(Color.WHITE);

        appName.setFont(
                new Font("SansSerif", Font.BOLD, 14));

        topBar.add(appName, BorderLayout.WEST);

        // User information and logout.
        JPanel userDetails = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 12, 0));

        userDetails.setOpaque(false);

        JLabel roleLabel =
                new JLabel(user.getRole().getDisplayName());

        roleLabel.setForeground(
                new Color(190, 216, 255));

        JLabel nameLabel =
                new JLabel(user.getFullName());

        nameLabel.setForeground(Color.WHITE);

        JButton logoutButton = new JButton("Logout");

        logoutButton.addActionListener(event -> {

            dispose();
            openLogin.run();
        });

        userDetails.add(roleLabel);
        userDetails.add(nameLabel);
        userDetails.add(logoutButton);

        topBar.add(userDetails, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // ===========================
        // Empty reusable sidebar
        // ===========================

        add(sidebar, BorderLayout.WEST);

        // ===========================
        // Empty dashboard content
        // ===========================

        // No placeholder pages are created.
        contentArea.setBackground(Color.WHITE);

        add(contentArea, BorderLayout.CENTER);
    }

    /**
     * Registers a module provided by a team member.
     *
     * Automatically:
     * - Adds its JPanel to CardLayout.
     * - Adds its navigation button to the sidebar.
     */
    public void registerModule(
            String name,
            JPanel modulePanel) {

        if (name == null
                || name.trim().isEmpty()
                || modulePanel == null) {

            throw new IllegalArgumentException(
                    "Module requires a name and JPanel.");
        }

        // Avoid duplicate module names.
        if (modules.containsKey(name)) {

            throw new IllegalArgumentException(
                    "Module already registered: " + name);
        }

        modules.put(name, modulePanel);

        // Register the module with CardLayout.
        contentArea.add(modulePanel, name);

        // Automatically create its sidebar button.
        sidebar.addModuleButton(
                name,
                () -> showModule(name));

        // Display the first registered module.
        if (modules.size() == 1) {
            showModule(name);
        }
    }

    /**
     * Switches to the selected registered module.
     */
    public void showModule(String name) {

        if (!modules.containsKey(name)) {

            throw new IllegalArgumentException(
                    "Unknown module: " + name);
        }

        cardLayout.show(contentArea, name);

        // Update sidebar highlighting.
        sidebar.setActiveModule(name);
    }
}