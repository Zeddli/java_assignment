package gui;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import service.AttendanceOverviewService.ClassStatus;

/**
 * Stacked bars, one per class: present (green) + late (amber) + absent (red).
 * Draws only what it is given, so it never reads files itself.
 */
public class AttendanceBarChart extends JPanel {
    private List<ClassStatus> data = new ArrayList<>();

    public AttendanceBarChart() {
        setOpaque(false);
        setPreferredSize(new Dimension(300, 190));
    }

    public void setData(List<ClassStatus> data) {
        this.data = data;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        int left = 26, right = 6, top = 22, bottom = 34;
        int plotW = w - left - right, plotH = h - top - bottom;

        // legend
        int lx = left;
        String[] names = {"Present", "Late", "Absent"};
        Color[] cols = {AttendanceTheme.PRESENT, new Color(0xE0A100), AttendanceTheme.ABSENT};
        g2.setFont(getFont().deriveFont(11f));
        FontMetrics fm = g2.getFontMetrics();
        for (int i = 0; i < 3; i++) {
            g2.setColor(cols[i]);
            g2.fillRoundRect(lx, 5, 10, 10, 3, 3);
            g2.setColor(AttendanceTheme.TEXT_MUTED);
            g2.drawString(names[i], lx + 14, 14);
            lx += 14 + fm.stringWidth(names[i]) + 14;
        }

        if (data.isEmpty()) {
            g2.setColor(AttendanceTheme.TEXT_MUTED);
            g2.drawString("No attendance recorded yet.", left, top + plotH / 2);
            g2.dispose();
            return;
        }

        int max = 1;
        for (ClassStatus s : data) max = Math.max(max, s.getRecorded());
        int step = max <= 5 ? 1 : (max <= 10 ? 2 : 5);
        int top0 = ((max + step - 1) / step) * step; // round the scale up

        // grid lines + y labels
        for (int v = 0; v <= top0; v += step) {
            int y = top + plotH - (int) ((long) plotH * v / top0);
            g2.setColor(new Color(0xEFE9EB));
            g2.drawLine(left, y, left + plotW, y);
            g2.setColor(AttendanceTheme.TEXT_MUTED);
            String t = String.valueOf(v);
            g2.drawString(t, left - 6 - fm.stringWidth(t), y + 4);
        }

        int slot = plotW / data.size();
        int barW = Math.min(46, Math.max(14, slot - 16));
        for (int i = 0; i < data.size(); i++) {
            ClassStatus s = data.get(i);
            int x = left + i * slot + (slot - barW) / 2;
            int base = top + plotH;
            int[] parts = {s.getPresent(), s.getLate(), s.getAbsent()};
            for (int k = 0; k < 3; k++) {
                if (parts[k] == 0) continue;
                int ph = (int) Math.max(2, (long) plotH * parts[k] / top0);
                g2.setColor(cols[k]);
                g2.fillRect(x, base - ph, barW, ph);
                if (ph >= 14) {
                    g2.setColor(Color.WHITE);
                    String t = String.valueOf(parts[k]);
                    g2.drawString(t, x + (barW - fm.stringWidth(t)) / 2, base - ph + (ph + fm.getAscent()) / 2 - 2);
                }
                base -= ph;
            }
            g2.setColor(AttendanceTheme.TEXT);
            String id = s.getSession().getId();
            g2.drawString(id, x + (barW - fm.stringWidth(id)) / 2, top + plotH + 14);
            g2.setColor(AttendanceTheme.TEXT_MUTED);
            String date = s.getSession().getDate().substring(5); // MM-dd
            g2.drawString(date, x + (barW - fm.stringWidth(date)) / 2, top + plotH + 27);
        }
        g2.dispose();
    }
}
