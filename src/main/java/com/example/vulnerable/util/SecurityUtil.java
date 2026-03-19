package com.example.vulnerable.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class SecurityUtil {

    // VULNERABILITY: Hardcoded encryption key
    private static final String SECRET_KEY = "1234567890123456";

    // VULNERABILITY: Weak encryption algorithm (DES)
    private static final String ALGORITHM = "DES";

    // VULNERABILITY: Using ECB mode (insecure)
    public static String encrypt(String data) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            e.printStackTrace();  // VULNERABILITY: Stack trace exposure
            return null;
        }
    }

    public static String decrypt(String encryptedData) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decoded = Base64.getDecoder().decode(encryptedData);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // VULNERABILITY: SQL Injection helper
    public static String buildQuery(String table, String condition) {
        return "SELECT * FROM " + table + " WHERE " + condition;  // No parameterization
    }

    // VULNERABILITY: Regex DoS (ReDoS)
    public static boolean validateEmail(String email) {
        String regex = "^([a-zA-Z0-9_\\-\\.]+)@([a-zA-Z0-9_\\-\\.]+)\\.([a-zA-Z]{2,5})$";
        // Complex regex that can cause catastrophic backtracking
        String complexRegex = "^(([a-z])+.)+[A-Z]([a-z])+$";
        return email.matches(regex);
    }
}
