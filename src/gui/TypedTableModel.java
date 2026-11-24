package gui;

/**
 * Read-only table model that reports the real class of each column (Integer, Double, String).
 * Without this a JTable sorts every column as text, so "10" would come before "9".
 */
public class TypedTableModel extends ReadOnlyTableModel {
    public TypedTableModel(String... columns) { super(columns); }

    @Override
    public Class<?> getColumnClass(int column) {
        if (getRowCount() > 0 && getValueAt(0, column) != null) return getValueAt(0, column).getClass();
        return Object.class;
    }
}
