package gui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.ToolTipManager;
import service.OverviewService.MonthTotal;

/**
 * Interactive bar chart of payments per month, drawn with Java2D (no external libraries).
 * - Hover a bar: it darkens and a tooltip shows "October 2026: 16 payments, RM 700.00".
 * - Click a bar: the click handler receives that month (the Overview opens Payment Records for it).
 * POLYMORPHISM: overrides paintComponent and getToolTipText(MouseEvent).
 */
public class MonthlyBarChart extends JComponent {
    private static final int TOP = 26, BOTTOM = 26, LEFT = 70, RIGHT = 14;

    private List<MonthTotal> data = new ArrayList<>();
    private final List<Rectangle> bars = new ArrayList<>(); // hit areas, rebuilt on every paint
    private int hoverIndex = -1;
    private Consumer<MonthTotal> onBarClicked = m -> { };

    public MonthlyBarChart() {
        setPreferredSize(new Dimension(400, 190));
        ToolTipManager.sharedInstance().registerComponent(this);
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { setHover(indexAt(e.getPoint())); }
            @Override public void mouseExited(MouseEvent e) { setHover(-1); }
            @Override public void mouseClicked(MouseEvent e) {
                int i = indexAt(e.getPoint());
                if (i >= 0) onBarClicked.accept(data.get(i));
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    public void setData(List<MonthTotal> data) {
        this.data = new ArrayList<>(data);
        repaint();
    }

    public void setOnBarClicked(Consumer<MonthTotal> handler) { this.onBarClicked = handler; }

    private void setHover(int i) {
        if (i == hoverIndex) return;
        hoverIndex = i;
        setCursor(Cursor.getPredefinedCursor(i >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        repaint();
    }

    private int indexAt(Point p) {
        for (int i = 0; i < bars.size(); i++) {
            Rectangle r = bars.get(i);
            // the whole column above the bar counts, so short bars are still easy to click
            if (p.x >= r.x && p.x <= r.x + r.width && p.y >= TOP && p.y <= getHeight() - BOTTOM) return i;
        }
        return -1;
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        int i = indexAt(e.getPoint());
        if (i < 0) return null;
        MonthTotal m = data.get(i);
        String name = m.getMonth().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + m.getMonth().getYear();
        return "<html><b>" + name + "</b><br>" + m.getCount() + " payment" + (m.getCount() == 1 ? "" : "s")
                + " · RM " + String.format("%,.2f", m.getTotal()) + "<br><i>Click to view these payments</i></html>";
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        int plotW = w - LEFT - RIGHT, plotH = h - TOP - BOTTOM;
        bars.clear();

        // white rounded card behind the chart
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(0, 0, w - 1, h - 1, 14, 14);
        g2.setColor(AdminTheme.BORDER);
        g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
        if (data.isEmpty() || plotW <= 0 || plotH <= 0) { g2.dispose(); return; }

        double max = niceMax(data.stream().mapToDouble(MonthTotal::getTotal).max().orElse(0));
        Font small = getFont().deriveFont(11f);
        g2.setFont(small);
        FontMetrics fm = g2.getFontMetrics();

        // horizontal grid lines with RM labels (0, half, max)
        for (int i = 0; i <= 2; i++) {
            double v = max * i / 2;
            int y = TOP + plotH - (int) Math.round(plotH * (v / max));
            g2.setColor(new Color(0xEEF1F4));
            g2.drawLine(LEFT, y, w - RIGHT, y);
            g2.setColor(AdminTheme.TEXT_MUTED);
            String label = "RM " + String.format("%,.0f", v);
            g2.drawString(label, LEFT - 8 - fm.stringWidth(label), y + fm.getAscent() / 2 - 1);
        }

        int n = data.size();
        int slot = plotW / n;
        int barW = Math.max(14, Math.min(54, (int) (slot * 0.55)));
        for (int i = 0; i < n; i++) {
            MonthTotal m = data.get(i);
            int x = LEFT + i * slot + (slot - barW) / 2;
            int barH = m.getTotal() <= 0 ? 2 : Math.max(3, (int) Math.round(plotH * (m.getTotal() / max)));
            int y = TOP + plotH - barH;
            bars.add(new Rectangle(x, y, barW, barH));

            boolean hot = i == hoverIndex;
            g2.setColor(m.getTotal() <= 0 ? AdminTheme.BORDER : hot ? AdminTheme.ACCENT_HOVER : AdminTheme.ACCENT);
            g2.fillRoundRect(x, y, barW, barH, 6, 6);
            if (hot) {
                g2.setColor(new Color(0x12, 0x38, 0x5E, 40)); // soft column highlight
                g2.fillRoundRect(LEFT + i * slot + 2, TOP - 4, slot - 4, plotH + 8, 8, 8);
            }

            // value above the bar, month name below
            String value = m.getTotal() <= 0 ? "-" : "RM " + String.format("%,.0f", m.getTotal());
            g2.setColor(hot ? AdminTheme.ACCENT : AdminTheme.TEXT);
            g2.setFont(small.deriveFont(hot ? Font.BOLD : Font.PLAIN));
            fm = g2.getFontMetrics();
            g2.drawString(value, x + (barW - fm.stringWidth(value)) / 2, y - 5);
            String month = m.getMonth().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            g2.setColor(AdminTheme.TEXT_MUTED);
            g2.drawString(month, x + (barW - fm.stringWidth(month)) / 2, TOP + plotH + fm.getAscent() + 6);
        }
        g2.dispose();
    }

    /** Rounds the top of the scale up to a tidy number (e.g. 1,370 -> 1,500) so labels look clean. */
    static double niceMax(double v) {
        if (v <= 0) return 100;
        double magnitude = Math.pow(10, Math.floor(Math.log10(v)));
        for (double step : new double[]{1, 1.5, 2, 2.5, 3, 4, 5, 6, 8, 10}) {
            if (step * magnitude >= v) return step * magnitude;
        }
        return 10 * magnitude;
    }
}
