package com.checkmarx.flow.utils;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class AesEncryptionUtilsTest {

    @Test
    void encrypt_nullInput_nullOutput() throws IOException {
        String cryptoText = AesEncryptionUtils.encrypt(null);
        assertTrue(StringUtils.isEmpty(cryptoText), "Crypto text must be empty.");
    }

    @Test
    void encrypt_nonEmptyInput_nonEmptyResult() throws IOException {
        String cryptoText = AesEncryptionUtils.encrypt("mytext");
        assertTrue(StringUtils.isNotEmpty(cryptoText), "Expected the encrypted text to be non-empty.");
    }

    @Test
    void encrypt_sameInput_differentCryptoTextEachTime() throws IOException {
        final String TEXT = "some long and fascinating text.";
        String encrypted1 = AesEncryptionUtils.encrypt(TEXT);
        String encrypted2 = AesEncryptionUtils.encrypt(TEXT);
        assertFalse(StringUtils.equals(encrypted1,
                encrypted2), "Expecting different encrypted text after each call.");
    }

    @Test
    void decrypt_validInput_validOutput() throws IOException {
        String cryptoText = "5euNblM8scCHnrmHpYuXszzdp6PG93Ac45tYup1F+YeWSU3q/uVRGZHs1CJIOb4my9164zSJGveixvkVWFU8Wg==";
        String expectedPlainText = "I've been waiting so long.";
        String decrypted = AesEncryptionUtils.decrypt(cryptoText);
        assertEquals(expectedPlainText, decrypted, "Unexpected plain text.");
    }

    @Test
    void decrypt_emptyInput_nullResult() throws IOException {
        final String MESSAGE = "Expecting null decryption result.";

        String plainText = AesEncryptionUtils.decrypt(null);
        assertNull(plainText, MESSAGE);

        plainText = AesEncryptionUtils.decrypt("");
        assertNull(plainText, MESSAGE);
    }

    @Test
    void decrypt_invalidInput_exception() {
        assertThrows(IOException.class,
                () -> AesEncryptionUtils.decrypt("definitely not encrypted"),
                "Expecting exception on invalid input.");
    }

    @Test
    void decrypt_encrypt() throws IOException {
        String plainText = "And now for something completely different";
        String encrypted = AesEncryptionUtils.encrypt(plainText);
        String decryptedAgain = AesEncryptionUtils.decrypt(encrypted);
        assertEquals(plainText, decryptedAgain, "Encrypt and then decrypt doesn't result in the same text.");
    }
}