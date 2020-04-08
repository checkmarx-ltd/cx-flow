package com.checkmarx.flow.utils;


import org.apache.ivy.util.StringUtils;
import org.jasypt.util.text.AES256TextEncryptor;

import java.io.IOException;

public class AesEncryptionUtils {
    private static final String KEY_VALUE = "Tx82^Fxl2pPoWK%m";

    private AesEncryptionUtils() {
    }

    /**
     * Encrypt a string with AES algorithm.
     */
    public static String encrypt(String plainText) throws IOException {
        return cryptoTransform(plainText, true);
    }

    /**
     * Decrypt a string with AES algorithm.
     */
    public static String decrypt(String cryptoText) throws IOException {
        return cryptoTransform(cryptoText, false);
    }

    private static String cryptoTransform(String input, boolean doEncrypt) throws IOException {
        try {
            if (!StringUtils.isNullOrEmpty(input)) {
                AES256TextEncryptor encryptor = new AES256TextEncryptor();
                encryptor.setPassword(KEY_VALUE);
                return doEncrypt ? encryptor.encrypt(input) : encryptor.decrypt(input);
            } else {
                return null;
            }
        } catch (Exception e) {
            String message = doEncrypt ? "Encryption error." : "Decryption error.";
            throw new IOException(message, e);
        }
    }
}
