package gui;

import java.awt.Component;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Read-only JTable whose columns sort when the header is clicked (extra feature).
 * Numbers are stored as Integer/Double so they sort as numbers, not text.
 * Doubles are shown as money with 2 decimals.
 * INHERITANCE: extends JTable; ReadOnlyTableModel is reused for the data.
 */
public class SortableTable extends JTable {

    public SortableTable(String... columns) {
        super(new TypedTableModel(columns));
        setAutoCreateRowSorter(true);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setRowHeight(24);
        getTableHeader().setReorderingAllowed(false);
        setFillsViewportHeight(true);
        setDefaultRenderer(Double.class, new MoneyRenderer());
    }

    /** Preferred width of each column in pixels (extra space is shared out proportionally). */
    public void setColumnWidths(int... widths) {
        for (int i = 0; i < widths.length && i < getColumnCount(); i++) {
            getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    public ReadOnlyTableModel model() { return (ReadOnlyTableModel) getModel(); }

    /** Removes all rows and the current sort/selection. */
    public void clearRows() {
        clearSelection();
        model().setRowCount(0);
    }

    public void addRow(Object... values) { model().addRow(values); }

    /** Value of a column in the selected row (after sorting), or null if no row is selected. */
    public Object selectedValue(int column) {
        int view = getSelectedRow();
        if (view < 0) return null;
        return model().getValueAt(convertRowIndexToModel(view), column);
    }

    /** Tells the sorter each column's real type, so 100.00 sorts after 40.00. */
    private static class TypedTableModel extends ReadOnlyTableModel {
        TypedTableModel(String... columns) { super(columns); }

        @Override
        public Class<?> getColumnClass(int column) {
            for (int r = 0; r < getRowCount(); r++) {
                Object v = getValueAt(r, column);
                if (v != null) return v.getClass();
            }
            return Object.class;
        }
    }

    /** POLYMORPHISM: overrides how a cell is drawn for Double values. */
    private static class MoneyRenderer extends DefaultTableCellRenderer {
        MoneyRenderer() { setHorizontalAlignment(SwingConstants.RIGHT); }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean sel,
                                                       boolean focus, int row, int col) {
            Object text = value instanceof Double ? String.format("%,.2f", (Double) value) : value;
            return super.getTableCellRendererComponent(t, text, sel, focus, row, col);
        }
    }
}
