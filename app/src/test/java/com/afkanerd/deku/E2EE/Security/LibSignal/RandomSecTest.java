package com.afkanerd.deku.E2EE.Security.LibSignal;

import static org.junit.Assert.assertArrayEquals;

import android.util.Base64;

import com.afkanerd.deku.E2EE.Security.SecurityHandler;
import com.google.crypto.tink.subtle.Hkdf;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

public class RandomSecTest {

    @Test
    public void HKDFTest() throws GeneralSecurityException {
        String algo = "HMACSHA512";

        byte[] ikm = "b863650041e1183fe909920e5d3c82e43393824b0274c2690b68b5d955070075".getBytes();
        byte[] salt = "43f8097fe98fbd2a051a625308f38e376781f012f25888c4733536a3244cb7ab".getBytes();

        byte[] infoRk = "kdf_rk".getBytes(), infoCk = "kdf_ck".getBytes();
        byte[] HkdfRkOut = Hkdf.computeHkdf(algo, ikm, salt, infoRk, 32);
        byte[] HkdfCkOut = Hkdf.computeHkdf(algo, ikm, salt, infoCk, 32);

        byte[] expectedOutRk = "xYRlJ2/Am68jgq+vW2Q+iEQhvE0BarTYrsxvq3tg/xQ=".getBytes();
        byte[] expectedOutCk = "FxE8p7KnF8f+0lh2NuTDQE4l8cblrDsCaI3q7ouDPIg=".getBytes();

        assertArrayEquals(expectedOutRk,
                com.google.crypto.tink.subtle.Base64.encode(HkdfRkOut,
                Base64.NO_WRAP));

        assertArrayEquals(expectedOutCk,
                com.google.crypto.tink.subtle.Base64.encode(HkdfCkOut,
                        Base64.NO_WRAP));
    }

    @Test
    public void customHKDFTest() throws GeneralSecurityException {
        String algo = "HMACSHA512";
        byte[] ikm = "b863650041e1183fe909920e5d3c82e43393824b0274c2690b68b5d955070075".getBytes();
        byte[] salt = "43f8097fe98fbd2a051a625308f38e376781f012f25888c4733536a3244cb7ab".getBytes();

        int len = 32;
        int num = 2;

        byte[] info = "kdf_ck".getBytes();
        byte[][] hkdfOutput = SecurityHandler.HKDF(algo, ikm, salt, info, len, num);

        byte[][] expectedOut = new byte[num][len];
        expectedOut[0] = com.google.crypto.tink.subtle.Base64.decode(
                "FxE8p7KnF8f+0lh2NuTDQE4l8cblrDsCaI3q7ouDPIg=".getBytes(), Base64.NO_WRAP);
        expectedOut[1] = com.google.crypto.tink.subtle.Base64.decode(
                "dQ6vtJ394Y4OhPM4iiLXw0vVjCPoDMzd288BNHJ64gE=".getBytes(), Base64.NO_WRAP);
        assertArrayEquals(expectedOut, hkdfOutput);
    }
}
