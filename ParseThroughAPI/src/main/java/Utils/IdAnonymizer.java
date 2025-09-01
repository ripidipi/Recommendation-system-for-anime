package Utils;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public class IdAnonymizer {
    private static final byte[] SECRET_KEY = loadSecretKey();

    private static byte[] loadSecretKey() {
        String envKey = generateHmacKey();
        if (envKey != null) return envKey.getBytes();

        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return key;
    }

    public static String anonymizeId(int originalId) {
        try {
            System.out.println(Arrays.toString(SECRET_KEY));
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET_KEY, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(String.valueOf(originalId).getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Error ananymization ", e);
        }
    }

    public static String generateHmacKey() {
        try {
            KeyGenerator kg = KeyGenerator.getInstance("HmacSHA256");
            SecretKey key = kg.generateKey();
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("Error key generation ", e);
        }
    }

}