package utils;

import exeptions.HashMakingError;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class IdAnonymizer {
    private static final byte[] SECRET_KEY = loadSecretKey();
    private static final String HASH_ALGORITHM = "HmacSHA512";
    private static final int KEY_LENGTH = 64;

    private IdAnonymizer() {}

    private static byte[] loadSecretKey() {
        String envKey = generateHmacKey();
        if (envKey != null) return envKey.getBytes();

        byte[] key = new byte[KEY_LENGTH];
        new SecureRandom().nextBytes(key);
        return key;
    }

    public static String anonymizeId(String originalId) {
        try {
            Mac mac = Mac.getInstance(HASH_ALGORITHM);
            mac.init(new SecretKeySpec(SECRET_KEY, HASH_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(String.valueOf(originalId).getBytes()));
        } catch (Exception e) {
            throw new HashMakingError("Error ananymization ", e);
        }
    }

    public static String generateHmacKey() {
        try {
            KeyGenerator kg = KeyGenerator.getInstance(HASH_ALGORITHM);
            SecretKey key = kg.generateKey();
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (Exception e) {
            throw new HashMakingError("Error key generation ", e);
        }
    }

}