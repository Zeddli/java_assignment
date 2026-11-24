package gui;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

/**
 * One place for the Attendance Officer screens' look: colours, fonts, table style,
 * status badges and the attendance-rate bar. Change ACCENT here and every AO screen follows.
 * The shared top bar and sidebar are NOT touched.
 */
public final class AttendanceTheme {
    private AttendanceTheme() { }

    // ---------- colours ----------
    public static final Color ACCENT = new Color(0xA62D4A);        // crimson
    public static final Color ACCENT_HOVER = new Color(0xBF3A5B);
    public static final Color ACCENT_TINT = new Color(0xF8E6EB);
    public static final Color BORDER = new Color(0xDDD3D6);
    public static final Color TEXT = new Color(0x24292F);
    public static final Color TEXT_MUTED = new Color(0x667085);
    public static final Color ERROR = new Color(0xB00020);

    public static final Color PRESENT = new Color(0x1E7A4C);
    public static final Color PRESENT_BG = new Color(0xDCF2E5);
    public static final Color LATE = new Color(0x9A6700);
    public static final Color LATE_BG = new Color(0xFFF0C2);
    public static final Color ABSENT = new Color(0xB00020);
    public static final Color ABSENT_BG = new Color(0xFBDADF);
    public static final Color NEUTRAL = new Color(0x5F6368);
    public static final Color NEUTRAL_BG = new Color(0xE8EAED);
    public static final Color INFO = new Color(0x1D4F91);
    public static final Color INFO_BG = new Color(0xDCE8F8);

    /** An attendance rate below this percentage is flagged as low. */
    public static final double LOW_RATE = service.AttendanceOverviewService.LOW_RATE;

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

    /** Light bordered box with padding (filter bars, chart box). */
    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10));
    }

    /** One line of a filter bar: components side by side (never wrapped, so nothing gets clipped). */
    public static JPanel row(Component... parts) {
        JPanel r = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        r.setBackground(Color.WHITE);
        r.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (Component c : parts) r.add(c);
        return r;
    }

    /** White bordered filter bar made of stacked rows. */
    public static JPanel filterBar(JPanel... rows) {
        JPanel bar = new JPanel();
        bar.setLayout(new BoxLayout(bar, BoxLayout.Y_AXIS));
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        for (JPanel r : rows) bar.add(r);
        return bar;
    }

    /** Colour used for an attendance rate: red below LOW_RATE, amber below 90, otherwise green. */
    public static Color rateColor(double rate) {
        if (rate < LOW_RATE) return ABSENT;
        if (rate < 90) return LATE;
        return PRESENT;
    }

    // ---------- tables ----------
    /** Crimson header row, tinted selection, sortable columns. */
    public static void styleTable(JTable table) {
        table.setRowHeight(26);
        table.setSelectionBackground(ACCENT_TINT);
        table.setSelectionForeground(TEXT);
        table.setGridColor(new Color(0xF0EAEC));
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);
        JTableHeader header = table.getTableHeader();
        header.setDefaultRenderer(new HeaderRenderer());
        header.setPreferredSize(new Dimension(0, 28));
        header.setReorderingAllowed(false);
    }

    /** Shows PRESENT / LATE / ABSENT (and Saved / Changed / Not yet) as coloured pills. */
    public static void useStatusBadges(JTable table, int column) {
        table.getColumnModel().getColumn(column).setCellRenderer(new StatusBadgeRenderer());
    }

    /** Shows a number 0-100 as a coloured bar with the percentage written on it. */
    public static void useRateBars(JTable table, int column) {
        table.getColumnModel().getColumn(column).setCellRenderer(new RateBarRenderer());
    }

    /** Draws the table header cells. POLYMORPHISM: overrides how JTable paints a header cell. */
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

    /** Text -> {foreground, background} for the pills. */
    private static final Map<String, Color[]> PILLS = new HashMap<>();
    static {
        PILLS.put("PRESENT", new Color[]{PRESENT, PRESENT_BG});
        PILLS.put("LATE", new Color[]{LATE, LATE_BG});
        PILLS.put("ABSENT", new Color[]{ABSENT, ABSENT_BG});
        PILLS.put("Saved", new Color[]{PRESENT, PRESENT_BG});
        PILLS.put("Changed", new Color[]{INFO, INFO_BG});
        PILLS.put("Not yet", new Color[]{LATE, LATE_BG});
    }

    private static class StatusBadgeRenderer extends DefaultTableCellRenderer {
        private Color pillBg = NEUTRAL_BG;
        private boolean hasPill;

        StatusBadgeRenderer() { setOpaque(false); } // we paint the background ourselves

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean sel,
                                                       boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, value, sel, false, row, col);
            Color[] pill = value == null ? null : PILLS.get(value.toString());
            hasPill = pill != null;
            pillBg = hasPill ? pill[1] : NEUTRAL_BG;
            setForeground(hasPill ? pill[0] : TEXT_MUTED);
            setFont(t.getFont().deriveFont(hasPill ? Font.BOLD : Font.PLAIN, t.getFont().getSize2D() - 1f));
            setHorizontalAlignment(CENTER);
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            if (hasPill) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                FontMetrics fm = g2.getFontMetrics(getFont());
                int w = Math.min(getWidth() - 6, fm.stringWidth(getText()) + 18);
                int h = Math.min(getHeight() - 6, fm.getHeight() + 2);
                g2.setColor(pillBg);
                g2.fillRoundRect((getWidth() - w) / 2, (getHeight() - h) / 2, w, h, h, h);
                g2.dispose();
            }
            super.paintComponent(g); // text only, because the label is not opaque
        }
    }

    private static class RateBarRenderer implements TableCellRenderer {
        private final JPanel cell = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int pad = 6, h = Math.min(16, getHeight() - 8), y = (getHeight() - h) / 2;
                int w = getWidth() - 2 * pad;
                g2.setColor(new Color(0xEEEAEB));
                g2.fillRoundRect(pad, y, w, h, h, h);
                g2.setColor(rateColor(rate));
                g2.fillRoundRect(pad, y, Math.max(h, (int) (w * Math.min(100, rate) / 100)), h, h, h);
                String text = String.format("%.1f%%", rate);
                g2.setFont(getFont().deriveFont(Font.BOLD, 11f));
                FontMetrics fm = g2.getFontMetrics();
                int barW = Math.max(h, (int) (w * Math.min(100, rate) / 100));
                int ty = y + (h + fm.getAscent() - fm.getDescent()) / 2;
                if (barW >= fm.stringWidth(text) + 14) {      // fits inside the bar: white text
                    g2.setColor(Color.WHITE);
                    g2.drawString(text, pad + 8, ty);
                } else {                                      // short bar: dark text just after it
                    g2.setColor(AttendanceTheme.TEXT);
                    g2.drawString(text, pad + barW + 6, ty);
                }
                g2.dispose();
            }
        };
        private double rate;

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean sel,
                                                       boolean focus, int row, int col) {
            rate = value instanceof Number ? ((Number) value).doubleValue() : 0;
            cell.setBackground(sel ? ACCENT_TINT : t.getBackground());
            cell.setFont(t.getFont());
            return cell;
        }
    }
}
