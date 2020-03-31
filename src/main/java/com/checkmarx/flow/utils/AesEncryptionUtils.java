package com.checkmarx.flow.utils;


import com.checkmarx.sdk.exception.CheckmarxException;
import com.google.common.base.Strings;
import org.eclipse.jgit.util.Base64;


import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;

public class AesEncryptionUtils {
    private static final String ALGO = "AES";
    private static final byte[] keyValue =
            new byte[]{'T', 'x', '8', '2', '^', 'F', 'x', 'l', '2', 'p', 'P', 'o', 'W', 'K', '%', 'm'};

    private AesEncryptionUtils(){}
    
    /**
     * Encrypt a string with AES algorithm.
     *
     * @param data is a string
     * @return the encrypted string
     */
    public static String encrypt(String data) throws CheckmarxException {
        try {
            Key key = generateKey();
            Cipher c = Cipher.getInstance(ALGO);
            c.init(Cipher.ENCRYPT_MODE, key);
            byte[] encVal = c.doFinal(data.getBytes());
            
            return Base64.encodeBytes(encVal);
        } catch (Exception e) {
            throw new CheckmarxException("Error encoding : " + e.getMessage());
        }
    }

    /**
     * Decrypt a string with AES algorithm.
     *
     * @param encodedData is a string
     * @return the decrypted string
     */
    public static String decrypt(String encodedData) throws CheckmarxException {
        try {
            if (!Strings.isNullOrEmpty(encodedData)) {
                Key key = generateKey();
                Cipher c = Cipher.getInstance(ALGO);
                c.init(Cipher.DECRYPT_MODE, key);
                byte[] decordedValue = Base64.decode(encodedData);
                byte[] decValue = c.doFinal(decordedValue);
                return new String(decValue);
            }else{
                return null;
            }
        } catch (Exception e) {
            throw new CheckmarxException("Error decoding : " + e.getMessage());
        }
    }

    /**
     * Generate a new encryption key.
     */
    private static Key generateKey()  {
        return new SecretKeySpec(keyValue, ALGO);
    }
}
