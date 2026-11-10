package gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import javax.swing.Icon;
import javax.swing.JOptionPane;

/**
 * Confirm and message pop-ups with SDAMS's own icons.
 * Without an explicit icon, macOS shows the application icon, which is Java's "Duke" mascot.
 */
public final class Dialogs {
    private Dialogs() { }

    /** The kind of pop-up decides the icon's colour and symbol. */
    public enum Kind {
        QUESTION(new Color(52, 94, 210), "?"),
        WARNING(new Color(214, 140, 0), "!"),
        INFO(new Color(52, 94, 210), "i");

        private final Color color;
        private final String symbol;

        Kind(Color color, String symbol) {
            this.color = color;
            this.symbol = symbol;
        }
    }

    /** Yes/No question. Returns true only when Yes is clicked. */
    public static boolean confirm(Component parent, String message, String title, Kind kind) {
        int choice = JOptionPane.showConfirmDialog(parent, message, title,
                JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, new SymbolIcon(kind));
        return choice == JOptionPane.YES_OPTION;
    }

    /** Message with a single OK button. */
    public static void inform(Component parent, String message, String title, Kind kind) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.PLAIN_MESSAGE, new SymbolIcon(kind));
    }

    /**
     * Draws the icon in code, so there is no image file to lose.
     * POLYMORPHISM: implements the Icon interface that JOptionPane draws.
     */
    private static class SymbolIcon implements Icon {
        private static final int SIZE = 44;
        private final Kind kind;

        SymbolIcon(Kind kind) { this.kind = kind; }

        @Override public int getIconWidth() { return SIZE; }
        @Override public int getIconHeight() { return SIZE; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(kind.color);
            if (kind == Kind.WARNING) {
                Polygon triangle = new Polygon(
                        new int[]{x + SIZE / 2, x + SIZE - 2, x + 2},
                        new int[]{y + 3, y + SIZE - 4, y + SIZE - 4}, 3);
                g2.fillPolygon(triangle);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawPolygon(triangle);
            } else {
                g2.fillOval(x + 2, y + 2, SIZE - 4, SIZE - 4);
            }
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 24));
            FontMetrics fm = g2.getFontMetrics();
            int tx = x + (SIZE - fm.stringWidth(kind.symbol)) / 2;
            int ty = y + (SIZE - fm.getHeight()) / 2 + fm.getAscent() + (kind == Kind.WARNING ? 4 : 0);
            g2.drawString(kind.symbol, tx, ty);
            g2.dispose();
        }
    }
}
