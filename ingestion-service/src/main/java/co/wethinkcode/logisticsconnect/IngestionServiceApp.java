package co.wethinkcode.logisticsconnect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import io.javalin.Javalin;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class IngestionServiceApp {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7050);

        app.get("/health", ctx -> ctx.result("OK"));
        app.get("/hubs", ctx -> ctx.json(loadAndCleanHubs()));
    }

    static List<Map<String, Object>> loadAndCleanHubs() {
        try (InputStream inputStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("hubs-global.csv");
             Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReader(reader)) {

            if (inputStream == null) {
                throw new IllegalStateException("hubs-global.csv was not found in the classpath");
            }

            List<String[]> rows = csvReader.readAll();
            if (rows.size() <= 1) {
                return List.of();
            }

            return cleanHubRecords(rows.subList(1, rows.size()));
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to read and clean hub CSV data", e);
        }
    }

    static List<Map<String, Object>> cleanHubRecords(List<String[]> rows) {
        Map<String, Map<String, Object>> deduplicated = new LinkedHashMap<>();

        for (String[] row : rows) {
            if (row == null || row.length < 4) {
                continue;
            }

            String hubId = normalizeHubId(row[0]);
            String province = normalizeProvince(row[1]);
            String sortingCenter = normalizeLabel(row[2]);
            Boolean active = normalizeBoolean(row[3]);

            if (hubId == null || province == null || sortingCenter == null) {
                continue;
            }

            String dedupeKey = (province + "|" + sortingCenter).toLowerCase(Locale.ROOT);
            Map<String, Object> normalizedRow = deduplicated.computeIfAbsent(dedupeKey, key -> new LinkedHashMap<>());

            normalizedRow.put("hubId", hubId);
            normalizedRow.put("province", province);
            normalizedRow.put("sortingCenter", sortingCenter);
            normalizedRow.put("active", active);
        }

        return new ArrayList<>(deduplicated.values());
    }

    private static String normalizeHubId(String rawValue) {
        if (rawValue == null) {
            return null;
        }

        String normalized = rawValue.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        String upper = normalized.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        return upper.matches("^[A-Z0-9-]+$") ? upper : null;
    }

    private static String normalizeProvince(String rawValue) {
        String normalized = normalizeLabel(rawValue);
        if (normalized == null || normalized.isEmpty()) {
            return null;
        }

        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.contains("gauteng")) return "Gauteng";
        if (lower.contains("western cape")) return "Western Cape";
        if (lower.contains("kwazulu") || lower.contains("kwa-zulu")) return "KwaZulu Natal";
        if (lower.contains("free state")) return "Free State";
        if (lower.contains("eastern cape")) return "Eastern Cape";
        if (lower.contains("north west")) return "North West";
        if (lower.contains("northern cape")) return "Northern Cape";
        if (lower.contains("limpopo")) return "Limpopo";
        if (lower.contains("mpumalanga")) return "Mpumalanga";

        return titleCase(normalized);
    }

    private static String normalizeLabel(String rawValue) {
        if (rawValue == null) {
            return null;
        }

        String normalized = rawValue.trim();
        if (normalized.isEmpty() || "N/A".equalsIgnoreCase(normalized) || "NA".equalsIgnoreCase(normalized)
                || "unknown".equalsIgnoreCase(normalized) || "tbd".equalsIgnoreCase(normalized)
                || "-".equals(normalized)) {
            return null;
        }

        normalized = normalized.replaceAll("\\s+", " ");
        normalized = normalized.replace("  ", " ");
        normalized = normalized.replace("-", " ");
        return titleCase(normalized);
    }

    private static String titleCase(String value) {
        String[] words = value.split(" ");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (words[i].isBlank()) {
                continue;
            }
            if (i > 0) {
                result.append(" ");
            }
            String word = words[i].trim();
            if (word.length() == 1) {
                result.append(word.toUpperCase(Locale.ROOT));
            } else {
                result.append(word.substring(0, 1).toUpperCase(Locale.ROOT))
                        .append(word.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return result.toString();
    }

    private static Boolean normalizeBoolean(String rawValue) {
        if (rawValue == null) {
            return false;
        }

        String value = rawValue.trim();
        if (value.isEmpty() || "N/A".equalsIgnoreCase(value) || "NA".equalsIgnoreCase(value)
                || "unknown".equalsIgnoreCase(value) || "tbd".equalsIgnoreCase(value) || "-".equals(value)) {
            return false;
        }

        if ("Y".equalsIgnoreCase(value) || "YES".equalsIgnoreCase(value) || "TRUE".equalsIgnoreCase(value)
                || "1".equals(value)) {
            return true;
        }

        return false;
    }
}
