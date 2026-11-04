import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared reader for pipe-delimited
 * text files used by SDAMS.
 */
public class TextFileHandler {

    public List<String[]> readRecords(Path filePath)
            throws IOException {

        List<String[]> records = new ArrayList<>();

        try (BufferedReader reader =
                Files.newBufferedReader(
                        filePath,
                        StandardCharsets.UTF_8)) {

            String line;

            while ((line = reader.readLine()) != null) {

                // if got blank line or comment line, skip it
                if (line.trim().isEmpty()
                        || line.trim().startsWith("#")) {
                    continue;
                }

                // split the line into fields 
                String[] fields = line.split("\\|", -1);

                records.add(fields);
            }
        }

        return records;
    }
}