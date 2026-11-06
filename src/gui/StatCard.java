package gui;

import java.awt.Font;
import javax.swing.*;
 
/** Small summary box: big number + caption. */
public class StatCard extends JPanel {
    private final JLabel value = new JLabel("0");
 
    public StatCard(String caption) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(java.awt.Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        value.setFont(value.getFont().deriveFont(Font.BOLD, 24f));
        add(value);
        add(new JLabel(caption));
    }
 
    public void setValue(String text) { value.setText(text); }
}

