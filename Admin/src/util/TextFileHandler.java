package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Unified text file utility supporting both String[] record reading 
 * for friend services and line-based file mutations for repositories.
 */
public final class TextFileHandler {

    private static final Path DIR = Paths.get("data");

    public TextFileHandler() { }

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

    /**
     * Friend's method: Reads pipe-delimited records directly into String[] lists.
     */
    public List<String[]> readRecords(Path filePath) throws IOException {
        List<String[]> records = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                    continue;
                }
                String[] fields = line.split("\\|", -1);
                records.add(fields);
            }
        }
        return records;
    }

    /**
     * Helper overload to accept plain file names (e.g. "users.txt").
     */
    public static List<String[]> readRecords(String fileName) {
        try {
            List<String[]> records = new ArrayList<>();
            for (String line : readLines(fileName)) {
                records.add(line.split("\\|", -1));
            }
            return records;
        } catch (Exception e) {
            throw new DataException("Cannot read records from " + fileName, e);
        }
    }

    /**
     * Returns non-empty, non-comment lines from a data file.
     */
    public static List<String> readLines(String fileName) {
        List<String> out = new ArrayList<>();
        for (String line : readRawLines(fileName)) {
            if (!line.isBlank() && !line.startsWith("#")) out.add(line);
        }
        return out;
    }

    /**
     * Reads all raw lines including comments (used when updating records).
     */
    public static List<String> readRawLines(String fileName) {
        try {
            return new ArrayList<>(Files.readAllLines(pathOf(fileName), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new DataException("Cannot read " + fileName, e);
        }
    }

    /**
     * Overwrites an entire file with updated lines.
     */
    public static void writeLines(String fileName, List<String> lines) {
        try {
            Files.write(pathOf(fileName), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DataException("Cannot write " + fileName, e);
        }
    }

    /**
     * Appends a single new record line to the end of a file.
     */
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