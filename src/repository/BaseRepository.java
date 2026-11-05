package repository;

import java.util.ArrayList;
import java.util.List;
import model.Storable;
import util.FileHandler;
 
/** Generic CRUD for one .txt file. Child classes only say how to build one object from a line. */
public abstract class BaseRepository<T extends Storable> {
    private final String fileName;
 
    protected BaseRepository(String fileName) { this.fileName = fileName; }
 
    protected abstract T fromFields(String[] fields);
 
    public List<T> findAll() {
        List<T> list = new ArrayList<>();
        for (String line : FileHandler.readLines(fileName)) {
            try {
                list.add(fromFields(line.split("\\|", -1)));
            } catch (RuntimeException badLine) {
                System.err.println("Skipping bad line in " + fileName + ": " + line);
            }
        }
        return list;
    }
 
    public T findById(String id) {
        for (T t : findAll()) if (t.getId().equals(id)) return t;
        return null;
    }
 
    public void add(T item) { FileHandler.appendLine(fileName, item.toLine()); }
 
    /** Replaces the line with the same id. Comment lines (#) and other records stay exactly as they were. */
    public boolean update(T item) {
        List<String> lines = new ArrayList<>();
        boolean found = false;
        for (String line : FileHandler.readRawLines(fileName)) {
            boolean isData = !line.isBlank() && !line.startsWith("#");
            if (isData && line.split("\\|", -1)[0].equals(item.getId())) {
                lines.add(item.toLine());
                found = true;
            } else {
                lines.add(line);
            }
        }
        if (found) FileHandler.writeLines(fileName, lines);
        return found;
    }
 
    /** Next id such as ATD004: prefix + biggest number so far + 1. */
    public String nextId(String prefix) {
        int max = 0;
        for (T t : findAll()) {
            if (t.getId().startsWith(prefix)) {
                try { max = Math.max(max, Integer.parseInt(t.getId().substring(prefix.length()))); }
                catch (NumberFormatException ignored) { }
            }
        }
        return String.format("%s%03d", prefix, max + 1);
    }
}

