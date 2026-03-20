package com.aquaa.edusoul.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import android.util.Base64; // For encoding salt and hash

/**
 * Utility class for handling password hashing and verification.
 * NOTE: This is a basic implementation using SHA-256. For production,
 * consider using stronger, dedicated password hashing libraries like Argon2 or SCrypt.
 */
public class PasswordUtils {

    private static final String HASHING_ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH_BYTES = 16; // 128 bits

    /**
     * Generates a salt and hashes the given password.
     *
     * @param password The plain text password.
     * @return A string containing the salt and hash, typically concatenated or stored separately.
     * For this example, we'll return "salt:hash" (Base64 encoded).
     */
    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            return null;
        }
        try {
            byte[] salt = generateSalt();
            byte[] hashedPassword = hash(password, salt);

            // Encode salt and hash to Base64 strings to safely store them
            String saltString = Base64.encodeToString(salt, Base64.NO_WRAP);
            String hashString = Base64.encodeToString(hashedPassword, Base64.NO_WRAP);

            return saltString + ":" + hashString; // Store as "salt:hashedPassword"
        } catch (NoSuchAlgorithmException e) {
            // Log error or handle appropriately in a real app
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Verifies a plain text password against a stored salt and hash.
     *
     * @param plainPassword      The password to verify.
     * @param storedSaltAndHash The stored string containing "salt:hash".
     * @return True if the password matches, false otherwise.
     */
    public static boolean verifyPassword(String plainPassword, String storedSaltAndHash) {
        if (plainPassword == null || plainPassword.isEmpty() || storedSaltAndHash == null || storedSaltAndHash.isEmpty()) {
            return false;
        }

        String[] parts = storedSaltAndHash.split(":");
        if (parts.length != 2) {
            // Invalid stored format
            return false;
        }

        try {
            byte[] salt = Base64.decode(parts[0], Base64.NO_WRAP);
            byte[] expectedHash = Base64.decode(parts[1], Base64.NO_WRAP);

            byte[] actualHash = hash(plainPassword, salt);
            return Arrays.equals(expectedHash, actualHash);

        } catch (NoSuchAlgorithmException | IllegalArgumentException e) {
            // IllegalArgumentException can be thrown by Base64.decode
            e.printStackTrace();
            return false;
        }
    }

    private static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        random.nextBytes(salt);
        return salt;
    }

    private static byte[] hash(String password, byte[] salt) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(HASHING_ALGORITHM);
        digest.reset();
        digest.update(salt); // Apply salt first
        byte[] saltedPasswordBytes = password.getBytes(StandardCharsets.UTF_8);
        return digest.digest(saltedPasswordBytes); // Then hash the password
    }

    // Example main for testing (not for Android app usage directly)
    public static void main(String[] args) {
        String myPassword = "password123";
        String hashedPasswordWithSalt = hashPassword(myPassword);
        System.out.println("Stored format (salt:hash): " + hashedPasswordWithSalt);

        // Verification
        System.out.println("Verification with correct password: " + verifyPassword(myPassword, hashedPasswordWithSalt));
        System.out.println("Verification with incorrect password: " + verifyPassword("wrongPassword", hashedPasswordWithSalt));
    }
}
