package com.hotel.channelmanager;

import com.hotel.channelmanager.common.security.EncryptionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionServiceTest {

    private final EncryptionService encryptionService = new EncryptionService("my-super-secret-master-key-32b!!");

    @Test
    @DisplayName("Should successfully encrypt and decrypt OTA credentials")
    void testEncryptDecrypt() {
        String sensitiveApiKey = "ota_live_key_99882233_booking_com";
        String cipherText = encryptionService.encrypt(sensitiveApiKey);

        assertNotNull(cipherText);
        assertNotEquals(sensitiveApiKey, cipherText);

        String decrypted = encryptionService.decrypt(cipherText);
        assertEquals(sensitiveApiKey, decrypted);
    }

    @Test
    @DisplayName("Random IV produces distinct ciphertexts for identical plaintext")
    void testUniqueCiphertextsDueToRandomIv() {
        String plaintext = "same_secret_password";
        String cipher1 = encryptionService.encrypt(plaintext);
        String cipher2 = encryptionService.encrypt(plaintext);

        assertNotEquals(cipher1, cipher2);
        assertEquals(plaintext, encryptionService.decrypt(cipher1));
        assertEquals(plaintext, encryptionService.decrypt(cipher2));
    }
}

