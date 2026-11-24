package gui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;

/**
 * Button that paints its own rounded background in the Admin colours.
 * Needed because the macOS look-and-feel ignores setBackground() on normal JButtons.
 * INHERITANCE: still a JButton (action listeners, enable/disable, keyboard all work).
 * POLYMORPHISM: overrides paintComponent to draw the custom look.
 */
public class AdminButton extends JButton {

    /** PRIMARY = filled navy, SECONDARY = navy outline, DANGER = red outline. */
    public enum Style { PRIMARY, SECONDARY, DANGER }

    private final Style style;
    private boolean hover;

    public AdminButton(String text, Style style) {
        super(text);
        this.style = style;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(getFont().deriveFont(Font.BOLD, 12.5f));
        // replace the look-and-feel border: macOS reserves wide padding even when it is not painted
        setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
            @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
        });
    }

    public AdminButton(String text) { this(text, Style.SECONDARY); }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth() - 1, h = getHeight() - 1, arc = 10;
        boolean on = isEnabled();
        boolean pressed = getModel().isPressed();
        Color main = style == Style.DANGER ? AdminTheme.ERROR : AdminTheme.ACCENT;

        if (style == Style.PRIMARY) {
            Color fill = !on ? new Color(0xA9B6C4) : (pressed || hover) ? AdminTheme.ACCENT_HOVER : AdminTheme.ACCENT;
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, w, h, arc, arc);
            setForeground(Color.WHITE);
        } else {
            g2.setColor(on && (hover || pressed) ? AdminTheme.ACCENT_TINT : Color.WHITE);
            g2.fillRoundRect(0, 0, w, h, arc, arc);
            g2.setColor(on ? main : AdminTheme.BORDER);
            g2.drawRoundRect(0, 0, w, h, arc, arc);
            setForeground(on ? main : new Color(0xA0A7B0));
        }
        g2.dispose();
        super.paintComponent(g); // draws the text
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(d.width, Math.max(d.height, 26));
    }
}
