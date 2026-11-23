package gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Clickable summary card for the Overview dashboard:
 *
 *   INSTRUCTORS
 *   4                      <- big number
 *   1 inactive             <- detail line
 *   Open list  >           <- appears in accent colour on hover
 *
 * Hovering tints the card; clicking it (or pressing Enter/Space when focused) runs the action.
 */
public class StatTile extends JPanel {
    private final JLabel lblValue = new JLabel("0");
    private final JLabel lblDetail = new JLabel(" ");
    private final JLabel lblOpen = new JLabel("Open list  ›");
    private final Runnable onOpen;
    private boolean hover;

    public StatTile(String title, String openText, Runnable onOpen) {
        this.onOpen = onOpen;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false); // background painted in paintComponent (rounded)
        setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText(openText);

        JLabel lblTitle = new JLabel(title.toUpperCase());
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 11f));
        lblTitle.setForeground(AdminTheme.TEXT_MUTED);
        lblValue.setFont(lblValue.getFont().deriveFont(Font.BOLD, 26f));
        lblValue.setForeground(AdminTheme.ACCENT);
        lblDetail.setForeground(AdminTheme.TEXT_MUTED);
        lblDetail.setFont(lblDetail.getFont().deriveFont(12f));
        lblOpen.setText(openText + "  ›");
        lblOpen.setFont(lblOpen.getFont().deriveFont(Font.BOLD, 11.5f));
        lblOpen.setForeground(new Color(0, 0, 0, 0)); // invisible until hover/focus
        for (JComponent c : new JComponent[]{lblTitle, lblValue, lblDetail, lblOpen}) {
            c.setAlignmentX(LEFT_ALIGNMENT);
            add(c);
        }

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { setHover(true); }
            @Override public void mouseExited(MouseEvent e) { setHover(false); }
            @Override public void mouseClicked(MouseEvent e) { requestFocusInWindow(); onOpen.run(); }
        });
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) onOpen.run();
            }
        });
        addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { repaint(); updateOpenLabel(); }
            @Override public void focusLost(FocusEvent e) { repaint(); updateOpenLabel(); }
        });
    }

    public void setValue(String value, String detail) {
        lblValue.setText(value);
        lblDetail.setText(detail == null || detail.isEmpty() ? " " : detail);
    }

    private void setHover(boolean on) {
        hover = on;
        updateOpenLabel();
        repaint();
    }

    private void updateOpenLabel() {
        lblOpen.setForeground(hover || isFocusOwner() ? AdminTheme.ACCENT : new Color(0, 0, 0, 0));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth() - 1, h = getHeight() - 1;
        g2.setColor(hover ? AdminTheme.ACCENT_TINT : Color.WHITE);
        g2.fillRoundRect(0, 0, w, h, 14, 14);
        boolean highlight = hover || isFocusOwner();
        g2.setColor(highlight ? AdminTheme.ACCENT : AdminTheme.BORDER);
        g2.setStroke(new BasicStroke(highlight ? 1.6f : 1f));
        g2.drawRoundRect(0, 0, w, h, 14, 14);
        g2.setColor(AdminTheme.ACCENT);           // accent strip on the left edge
        g2.fillRoundRect(0, 8, 4, h - 16, 4, 4);
        g2.dispose();
        super.paintComponent(g);
    }
}
