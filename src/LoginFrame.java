import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * Shared login screen.
 * Users can log in but cannot register here.
 */
public class LoginFrame extends JFrame {

    private final AuthService authService;

    private final JTextField usernameField = new JTextField();

    private final JPasswordField passwordField = new JPasswordField();

    private final JLabel errorLabel = new JLabel(" ");

    public LoginFrame(AuthService authService) {

        this.authService = authService;

        setTitle("SDAMS - Sign in");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setSize(960, 600);
        setMinimumSize(new Dimension(700, 500));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Main application heading.
        JPanel topBar = new JPanel(new BorderLayout());

        topBar.setBackground(new Color(30, 30, 30));

        topBar.setBorder(
                BorderFactory.createEmptyBorder(
                        11, 20, 11, 20));

        JLabel appName = new JLabel("SDAMS - Self Defence Academy");

        appName.setForeground(Color.WHITE);
        appName.setFont(
                new Font("SansSerif", Font.BOLD, 14));

        topBar.add(appName, BorderLayout.WEST);

        add(topBar, BorderLayout.NORTH);

        // Background for the centered login card.
        JPanel background = new JPanel(new GridBagLayout());

        background.setBackground(
                new Color(242, 243, 245));

        // Login card.
        JPanel card = new JPanel();

        card.setBackground(Color.WHITE);

        card.setLayout(
                new BoxLayout(card, BoxLayout.Y_AXIS));

        card.setPreferredSize(
                new Dimension(320, 420));

        card.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                new Color(190, 190, 190)),
                        BorderFactory.createEmptyBorder(
                                22, 24, 22, 24)));

        // Logo placeholder.
        JLabel logo = new JLabel("put logo image here", SwingConstants.CENTER);

        logo.setBorder(
                BorderFactory.createDashedBorder(Color.GRAY));

        logo.setPreferredSize(new Dimension(52, 52));
        logo.setMaximumSize(new Dimension(52, 52));
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(logo);
        card.add(Box.createVerticalStrut(16));

        // Sign-in heading.
        JLabel heading = new JLabel("Sign in");

        heading.setFont(
                new Font("SansSerif", Font.BOLD, 21));

        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(heading);
        card.add(Box.createVerticalStrut(15));

        // Username input.
        card.add(fieldLabel("Username"));
        card.add(Box.createVerticalStrut(4));

        styleInput(usernameField);

        usernameField.setToolTipText("Example: stu001");

        card.add(usernameField);
        card.add(Box.createVerticalStrut(12));

        // Password input.
        card.add(fieldLabel("Password"));
        card.add(Box.createVerticalStrut(4));

        styleInput(passwordField);

        card.add(passwordField);
        card.add(Box.createVerticalStrut(9));

        // Inline validation error.
        errorLabel.setFont(
                new Font("SansSerif", Font.PLAIN, 11));

        errorLabel.setForeground(
                new Color(185, 0, 0));

        errorLabel.setAlignmentX(
                Component.LEFT_ALIGNMENT);

        card.add(errorLabel);
        card.add(Box.createVerticalStrut(11));

        // Login button.
        JButton loginButton = new JButton("Login");

        loginButton.setBackground(
                new Color(30, 30, 30));

        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);

        loginButton.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 36));

        loginButton.setAlignmentX(
                Component.LEFT_ALIGNMENT);

        loginButton.addActionListener(
                event -> login());

        card.add(loginButton);
        card.add(Box.createVerticalStrut(12));

        // Informational note only. No registration feature.
        JLabel note = new JLabel(
                "<html>No self-registration: accounts are "
                        + "created by a Manager or Administrator.</html>");

        note.setFont(
                new Font("SansSerif", Font.PLAIN, 11));

        note.setForeground(Color.DARK_GRAY);
        note.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(note);

        background.add(card);

        add(background, BorderLayout.CENTER);

        // Allow the Enter key to submit the login form.
        getRootPane().setDefaultButton(loginButton);
    }

    /**
     * Creates a common field label.
     */
    private static JLabel fieldLabel(String text) {

        JLabel label = new JLabel(text);

        label.setFont(
                new Font("SansSerif", Font.PLAIN, 12));

        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        return label;
    }

    /**
     * Applies consistent sizing to input fields.
     */
    private static void styleInput(JTextField field) {

        field.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 34));

        field.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    /**
     * Validates the login fields, checks credentials,
     * and opens the correct dashboard.
     */
    private void login() {

        String username = usernameField.getText().trim();

        char[] password = passwordField.getPassword();

        try {

            // Clear previous error.
            errorLabel.setText(" ");

            // Validate required fields.
            if (username.isEmpty()
                    || password.length == 0) {

                errorLabel.setText(
                        "Username and password are required.");

                return;
            }

            // Pipe characters are not allowed by
            // the shared text-file contract.
            if (username.contains("|")) {

                errorLabel.setText(
                        "Invalid username.");

                return;
            }

            // Authenticate against users.txt.
            Optional<User> result = authService.login(username, password);

            // Invalid credentials or INACTIVE account.
            if (!result.isPresent()) {

                errorLabel.setText(
                        "Username or password is incorrect.");

                return;
            }

            // Redirect to the authenticated role.
            DashboardFrame dashboard = new DashboardFrame(
                    result.get(),
                    () -> new LoginFrame(authService)
                            .setVisible(true));

            // Register modules belonging to the logged-in role.
            ModuleSetup.register(dashboard, result.get());

            // Open the dashboard.
            dashboard.setVisible(true);

            dispose();

        } catch (IOException ex) {

            errorLabel.setText(
                    "Unable to read users.txt.");

            // Useful for debugging in Eclipse.
            ex.printStackTrace();

        } finally {

            // Clear the temporary password data.
            Arrays.fill(password, '\0');

            passwordField.setText("");
        }
    }
}