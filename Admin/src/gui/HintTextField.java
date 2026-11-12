package gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JTextField;

/**
 * Text field that shows grey placeholder text (e.g. "yyyy-MM-dd") while it is empty.
 * POLYMORPHISM: overrides paintComponent to draw the hint on top of the normal field.
 */
public class HintTextField extends JTextField {
    private final String hint;

    public HintTextField(String hint, int columns) {
        super(columns);
        this.hint = hint;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        paintHint(this, g, hint);
    }

    /** Draws grey hint text inside any empty text field (also used by the password field). */
    public static void paintHint(JTextField field, Graphics g, String hint) {
        if (hint == null || field.getDocument().getLength() > 0) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(Color.GRAY);
        g2.setFont(field.getFont().deriveFont(field.getFont().getSize2D() - 1f));
        int y = (field.getHeight() - g2.getFontMetrics().getHeight()) / 2 + g2.getFontMetrics().getAscent();
        g2.drawString(hint, field.getInsets().left, y);
        g2.dispose();
    }
}
