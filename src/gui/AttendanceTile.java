package gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Summary card for the Overview:
 *
 *   CLASSES TO MARK
 *   3                      <- big number (colour can change)
 *   9 students waiting     <- detail line
 *   Open  >                <- only when the card is clickable, shown on hover
 */
public class AttendanceTile extends JPanel {
    private final JLabel lblValue = new JLabel("0");
    private final JLabel lblDetail = new JLabel(" ");
    private final JLabel lblOpen = new JLabel(" ");
    private boolean hover;

    public AttendanceTile(String title, String openText, Runnable onOpen) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        JLabel lblTitle = new JLabel(title.toUpperCase());
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 11f));
        lblTitle.setForeground(AttendanceTheme.TEXT_MUTED);
        lblValue.setFont(lblValue.getFont().deriveFont(Font.BOLD, 26f));
        lblValue.setForeground(AttendanceTheme.ACCENT);
        lblDetail.setForeground(AttendanceTheme.TEXT_MUTED);
        lblDetail.setFont(lblDetail.getFont().deriveFont(12f));
        lblOpen.setFont(lblOpen.getFont().deriveFont(Font.BOLD, 11.5f));
        lblOpen.setForeground(new Color(0, 0, 0, 0)); // invisible until hover
        if (onOpen != null && openText != null) {
            lblOpen.setText(openText + "  ›");
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        for (JComponent c : new JComponent[]{lblTitle, lblValue, lblDetail, lblOpen}) {
            c.setAlignmentX(LEFT_ALIGNMENT);
            add(c);
        }
        if (onOpen != null) {
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { setHover(true); }
                @Override public void mouseExited(MouseEvent e) { setHover(false); }
                @Override public void mouseClicked(MouseEvent e) { onOpen.run(); }
            });
        }
    }

    /** Sets the big number, its colour and the small line under it. */
    public void setValue(String value, Color valueColor, String detail) {
        lblValue.setText(value);
        lblValue.setForeground(valueColor == null ? AttendanceTheme.ACCENT : valueColor);
        lblDetail.setText(detail == null || detail.isEmpty() ? " " : detail);
    }

    private void setHover(boolean on) {
        hover = on;
        lblOpen.setForeground(on ? AttendanceTheme.ACCENT : new Color(0, 0, 0, 0));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth() - 1, h = getHeight() - 1;
        g2.setColor(hover ? AttendanceTheme.ACCENT_TINT : Color.WHITE);
        g2.fillRoundRect(0, 0, w, h, 14, 14);
        g2.setColor(hover ? AttendanceTheme.ACCENT : AttendanceTheme.BORDER);
        g2.drawRoundRect(0, 0, w, h, 14, 14);
        g2.setColor(AttendanceTheme.ACCENT);          // accent strip on the left edge
        g2.fillRoundRect(0, 8, 4, h - 16, 4, 4);
        g2.dispose();
        super.paintComponent(g);
    }
}
