package gui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;

/**
 * Button that paints its own rounded background in the Attendance Officer colours.
 * Needed because some look-and-feels ignore setBackground() on normal JButtons.
 * INHERITANCE: still a JButton, so listeners, enable/disable and keyboard all work.
 * POLYMORPHISM: overrides paintComponent to draw the custom look.
 */
public class AttendanceButton extends JButton {

    /** PRIMARY = filled crimson, SECONDARY = crimson outline. */
    public enum Style { PRIMARY, SECONDARY }

    private final Style style;
    private boolean hover;

    public AttendanceButton(String text, Style style) {
        super(text);
        this.style = style;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(getFont().deriveFont(Font.BOLD, 12.5f));
        setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
            @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
        });
    }

    public AttendanceButton(String text) { this(text, Style.SECONDARY); }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth() - 1, h = getHeight() - 1, arc = 10;
        boolean on = isEnabled();
        boolean active = on && (hover || getModel().isPressed());
        if (style == Style.PRIMARY) {
            g2.setColor(!on ? new Color(0xD7B5BF) : active ? AttendanceTheme.ACCENT_HOVER : AttendanceTheme.ACCENT);
            g2.fillRoundRect(0, 0, w, h, arc, arc);
            setForeground(Color.WHITE);
        } else {
            g2.setColor(active ? AttendanceTheme.ACCENT_TINT : Color.WHITE);
            g2.fillRoundRect(0, 0, w, h, arc, arc);
            g2.setColor(on ? AttendanceTheme.ACCENT : AttendanceTheme.BORDER);
            g2.drawRoundRect(0, 0, w, h, arc, arc);
            setForeground(on ? AttendanceTheme.ACCENT : new Color(0xA0A7B0));
        }
        g2.dispose();
        super.paintComponent(g); // draws the text
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(d.width, Math.max(d.height, 28));
    }
}
