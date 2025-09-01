package Utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class IdAnonymizer {
    private static final String HMAC_SHA256 = "HmacSHA256";
    private final String SECRET_KEY;

    IdAnonymizer() {
        SECRET_KEY = RandomKeyGeneration.getKey(2048);
    }

    public String anonymizeId(Integer originalId) throws Exception {
        if (SECRET_KEY == null || SECRET_KEY.isEmpty()) {
            throw new IllegalArgumentException("HASH_SECRET_KEY environment variable is not set");
        }

        SecretKeySpec keySpec = new SecretKeySpec(
                SECRET_KEY.getBytes(StandardCharsets.UTF_8),
                HMAC_SHA256
        );

        Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(keySpec);

        byte[] hash = mac.doFinal(originalId.toString().getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

}