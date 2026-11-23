package gui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

/**
 * One place for the Administrator screens' look: colours, fonts, table style and status badges.
 * Every Admin screen asks this class for its styling, so changing the accent colour here
 * restyles all of them at once. The shared top bar and sidebar are NOT touched.
 */
public final class AdminTheme {
    private AdminTheme() { }

    // ---------- colours ----------
    /** Admin accent colour (#12385e, navy). */
    public static final Color ACCENT = new Color(0x12385E);
    /** Slightly lighter accent for hover. */
    public static final Color ACCENT_HOVER = new Color(0x1D5085);
    /** Very light accent tint for selected rows, card hover and soft backgrounds. */
    public static final Color ACCENT_TINT = new Color(0xE4ECF4);
    public static final Color BORDER = new Color(0xD5DCE4);
    public static final Color TEXT = new Color(0x1F2933);
    public static final Color TEXT_MUTED = new Color(0x5B6573);
    public static final Color ERROR = new Color(0xB00020);
    public static final Color SUCCESS = new Color(0x1E7A4C);
    public static final Color SUCCESS_BG = new Color(0xDCF2E5);
    public static final Color NEUTRAL = new Color(0x5F6368);
    public static final Color NEUTRAL_BG = new Color(0xE8EAED);

    // ---------- text ----------
    public static JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 20f));
        l.setForeground(ACCENT);
        return l;
    }

    public static JLabel sectionTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 14f));
        l.setForeground(ACCENT);
        return l;
    }

    public static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT_MUTED);
        return l;
    }

    /** White box with a light border and inner padding (used for forms and cards). */
    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10));
    }

    // ---------- tables ----------
    /** Accent header row, tinted selection, and status badges in the given column (-1 = none). */
    public static void styleTable(JTable table, int statusColumn) {
        table.setSelectionBackground(ACCENT_TINT);
        table.setSelectionForeground(TEXT);
        table.setGridColor(new Color(0xEDF0F3));
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        JTableHeader header = table.getTableHeader();
        header.setDefaultRenderer(new HeaderRenderer());
        header.setPreferredSize(new Dimension(0, 28));
        if (statusColumn >= 0) {
            table.getColumnModel().getColumn(statusColumn).setCellRenderer(new StatusBadgeRenderer());
        }
    }

    /**
     * Accent-coloured header cells with a sort arrow.
     * POLYMORPHISM: overrides how JTable draws each header cell.
     */
    private static class HeaderRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean sel,
                                                       boolean focus, int row, int col) {
            String text = value == null ? "" : value.toString();
            RowSorter<?> sorter = t.getRowSorter();
            if (sorter != null && !sorter.getSortKeys().isEmpty()) {
                RowSorter.SortKey key = sorter.getSortKeys().get(0);
                if (key.getColumn() == t.convertColumnIndexToModel(col)) {
                    text += key.getSortOrder() == SortOrder.ASCENDING ? "  ▲"
                          : key.getSortOrder() == SortOrder.DESCENDING ? "  ▼" : "";
                }
            }
            super.getTableCellRendererComponent(t, text, false, false, row, col);
            setBackground(ACCENT);
            setForeground(Color.WHITE);
            setFont(t.getFont().deriveFont(Font.BOLD));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 0, 1, ACCENT_HOVER),
                    BorderFactory.createEmptyBorder(0, 6, 0, 6)));
            return this;
        }
    }

    /** Draws ACTIVE as a green pill and INACTIVE as a grey pill. */
    public static TableCellRenderer statusBadge() { return new StatusBadgeRenderer(); }

    private static class StatusBadgeRenderer extends DefaultTableCellRenderer {
        private Color pillBg = NEUTRAL_BG;

        StatusBadgeRenderer() { setOpaque(false); } // we paint the background ourselves

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean sel,
                                                       boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, value, sel, false, row, col);
            boolean active = "ACTIVE".equals(value);
            pillBg = active ? SUCCESS_BG : NEUTRAL_BG;
            setForeground(active ? SUCCESS : NEUTRAL);
            setFont(t.getFont().deriveFont(Font.BOLD, t.getFont().getSize2D() - 1f));
            setHorizontalAlignment(CENTER);
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            // paint the row background, then the pill, then the text on top
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            FontMetrics fm = g2.getFontMetrics(getFont());
            int w = Math.min(getWidth() - 6, fm.stringWidth(getText()) + 16);
            int h = Math.min(getHeight() - 6, fm.getHeight() + 2);
            g2.setColor(pillBg);
            g2.fillRoundRect((getWidth() - w) / 2, (getHeight() - h) / 2, w, h, h, h);
            g2.dispose();
            super.paintComponent(g); // text only, because the label is not opaque
        }
    }
}
