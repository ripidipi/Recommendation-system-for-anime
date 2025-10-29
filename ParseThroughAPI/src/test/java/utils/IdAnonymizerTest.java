package utils;

import org.junit.jupiter.api.Test;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

class IdAnonymizerTest {

    @Test
    void generateHmacKey_returnsValidBase64And32Bytes() {
        String key = IdAnonymizer.generateHmacKey();
        assertAll("Generated key validity",
                () -> assertNotNull(key, "Key must not be null"),
                () -> {
                    byte[] decoded = Base64.getDecoder().decode(key);
                    assertEquals(64, decoded.length, "Key length must be 32 bytes");
                }
        );
    }

    @Test
    void anonymize_sameInput_sameOutput() {
        String id = "test-id";
        String a = IdAnonymizer.anonymizeId(id);
        String b = IdAnonymizer.anonymizeId(id);
        assertAll("Deterministic output",
                () -> assertNotNull(a, "Output must not be null"),
                () -> assertEquals(a, b, "Same input must produce same output")
        );
    }

    @Test
    void anonymize_differentInputs_differentOutputs() {
        String a = IdAnonymizer.anonymizeId("a");
        String b = IdAnonymizer.anonymizeId("b");
        assertAll("Different input -> different output",
                () -> assertNotNull(a),
                () -> assertNotNull(b),
                () -> assertNotEquals(a, b)
        );
    }

    @Test
    void anonymize_handlesNullSafely() {
        String fromNull = IdAnonymizer.anonymizeId(null);
        String fromStringNull = IdAnonymizer.anonymizeId("null");
        assertAll("Null handling",
                () -> assertNotNull(fromNull),
                () -> assertNotNull(fromStringNull),
                () -> assertEquals(fromNull, fromStringNull)
        );
    }

    @Test
    void anonymize_outputIsUrlSafeBase64() {
        String result = IdAnonymizer.anonymizeId("safe-check");
        assertAll("Output format",
                () -> assertNotNull(result),
                () -> assertTrue(result.matches("[A-Za-z0-9_-]+"), "Should contain only URL-safe Base64 chars"),
                () -> assertFalse(result.contains("="), "Should not have padding")
        );
    }

    @Test
    void anonymize_resultNotEqualToOriginal() {
        String original = "plain";
        String anon = IdAnonymizer.anonymizeId(original);
        assertAll("Anonymization check",
                () -> assertNotNull(anon),
                () -> assertNotEquals(original, anon)
        );
    }
}
