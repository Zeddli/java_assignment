package gui;

import java.awt.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.*;
import model.User;
import repository.UserRepository;

/**
 * Personalised header shown at the top of every Administrator screen:
 *
 *   Manage Staff                                     Good afternoon, Aisha Rahman
 *   Create, view, search, update and delete ...        Thursday, 20 November 2026
 *
 * The name is re-read from users.txt on refresh(), so it updates straight after
 * the admin edits their own name in My Profile.
 */
public class WelcomeHeader extends JPanel {
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH);

    private final String userId;
    private final UserRepository users = new UserRepository();
    private final JLabel lblGreeting = new JLabel();
    private final JLabel lblDate = new JLabel();

    public WelcomeHeader(String userId, String title, String subtitle) {
        this.userId = userId;
        setLayout(new BorderLayout(12, 0));
        setOpaque(false);

        lblGreeting.setForeground(AdminTheme.TEXT_MUTED);
        lblGreeting.setFont(lblGreeting.getFont().deriveFont(12.5f));
        lblDate.setForeground(AdminTheme.TEXT_MUTED);
        lblDate.setFont(lblDate.getFont().deriveFont(12.5f));
        // greeting and date stacked on the right, beside the title (saves a line of height)
        lblGreeting.setAlignmentX(RIGHT_ALIGNMENT);
        lblDate.setAlignmentX(RIGHT_ALIGNMENT);
        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.add(Box.createVerticalStrut(3));
        right.add(lblGreeting);
        right.add(lblDate);

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel heading = AdminTheme.heading(title);
        JLabel sub = AdminTheme.muted(subtitle);
        heading.setAlignmentX(LEFT_ALIGNMENT);
        sub.setAlignmentX(LEFT_ALIGNMENT);
        titles.add(heading);
        titles.add(sub);

        add(titles, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, AdminTheme.ACCENT), // accent line underneath
                BorderFactory.createEmptyBorder(0, 0, 6, 0)));
        refresh();
    }

    /** Re-reads the admin's name and today's date. */
    public final void refresh() {
        User me = users.findById(userId);
        String name = me == null ? "Administrator" : me.getFullName();
        lblGreeting.setText(greeting(LocalTime.now()) + ", " + name);
        lblDate.setText(LocalDate.now().format(DATE));
    }

    /** "Good morning" before 12:00, "Good afternoon" before 18:00, otherwise "Good evening". */
    static String greeting(LocalTime time) {
        if (time.getHour() < 12) return "Good morning";
        if (time.getHour() < 18) return "Good afternoon";
        return "Good evening";
    }
}
