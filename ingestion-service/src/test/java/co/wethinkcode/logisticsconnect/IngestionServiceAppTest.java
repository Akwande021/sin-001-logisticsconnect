package co.wethinkcode.logisticsconnect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class IngestionServiceAppTest {

    @Test
    void cleanHubRecords_normalizesWhitespaceCasingAndDeduplicatesDuplicates() {
        List<String[]> input = List.of(
            new String[] {"H-500", " Gauteng ", "Johannesburg Central", "Y"},
            new String[] {"h-501", "Western Cape", "Cape Town Port", "yes"},
            new String[] {"H-504", "Gauteng", "Johannesburg Central", "true"},
            new String[] {"H-505", "Western Cape ", "Cape Town  Port", "TRUE"}
        );

        List<Map<String, Object>> cleaned = IngestionServiceApp.cleanHubRecords(input);

        assertEquals(2, cleaned.size());
        assertTrue(cleaned.stream().anyMatch(row -> "Johannesburg Central".equals(row.get("sortingCenter"))
            && Boolean.TRUE.equals(row.get("active"))));
        assertTrue(cleaned.stream().anyMatch(row -> "Cape Town Port".equals(row.get("sortingCenter"))
            && Boolean.TRUE.equals(row.get("active"))));
    }
}
