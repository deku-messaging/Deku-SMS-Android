package com.example.swob_deku;

import com.example.swob_deku.Models.Security.SecurityAES;


import org.junit.Test;
import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;

public class SecurityUnitTest {

    @Test
    public void security_aes_256_cbc_can_encrypt() throws Throwable {
        byte[] input = "hello world".getBytes(StandardCharsets.UTF_8);
        byte[] secretKey = "1234567890123456".getBytes(StandardCharsets.UTF_8);
        byte[] iv = "1234567890123456".getBytes(StandardCharsets.UTF_8);

        byte[] encryptedOutput_withIv = SecurityAES.encrypt_256_cbc(input, secretKey, iv);

        byte[] outputIv = new byte[16];
        System.arraycopy(encryptedOutput_withIv, 0, outputIv, 0, outputIv.length);
        assertArrayEquals(outputIv, iv);

        // Would raise an exception if this fails
        byte[] encryptedOutput = SecurityAES.encrypt_256_cbc(input, secretKey, null);
    }

    @Test
    public void security_aes_256_cbc_can_decrypt() {
    }
}
