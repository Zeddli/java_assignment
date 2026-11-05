package util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
 
/** The ONLY class that touches the .txt files (shared by the whole team). */
public final class FileHandler {
    private static final Path DIR = Paths.get("data");
 
    private FileHandler() { }
 
    private static Path pathOf(String fileName) {
        try {
            Files.createDirectories(DIR);
            Path p = DIR.resolve(fileName);
            if (!Files.exists(p)) Files.createFile(p);
            return p;
        } catch (IOException e) {
            throw new DataException("Cannot prepare " + fileName, e);
        }
    }
 
    /** Data lines only: blank lines and comment lines (starting with #) are ignored. */
    public static List<String> readLines(String fileName) {
        List<String> out = new ArrayList<>();
        for (String line : readRawLines(fileName)) {
            if (!line.isBlank() && !line.startsWith("#")) out.add(line);
        }
        return out;
    }
 
    /** Every line exactly as stored, including comments (used when rewriting a file). */
    public static List<String> readRawLines(String fileName) {
        try {
            return new ArrayList<>(Files.readAllLines(pathOf(fileName), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new DataException("Cannot read " + fileName, e);
        }
    }
 
    public static void writeLines(String fileName, List<String> lines) {
        try {
            Files.write(pathOf(fileName), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DataException("Cannot write " + fileName, e);
        }
    }
 
    /** Adds one line at the end. If the file does not end with a new line yet, one is added first. */
    public static void appendLine(String fileName, String line) {
        try {
            Path p = pathOf(fileName);
            byte[] existing = Files.readAllBytes(p);
            String prefix = (existing.length > 0 && existing[existing.length - 1] != '\n') ? "\n" : "";
            Files.write(p, (prefix + line + "\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new DataException("Cannot append to " + fileName, e);
        }
    }
}

