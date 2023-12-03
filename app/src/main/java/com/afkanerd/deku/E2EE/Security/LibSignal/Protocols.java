package com.afkanerd.deku.E2EE.Security.LibSignal;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class Protocols {

    // TODO
    public int MAX_SKIP = 0;

    final int HKDF_LEN = 32;
    final int HKDF_NUM_KEYS = 2;

    public static KeyPair GENERATE_DH() {
        return null;
    }

    public static byte[] DH(KeyPair dhPair, PublicKey publicKey) {
        return null;
    }

    public static byte[][] KDF_RK(byte[] rk, byte[] dhOut) {
        return null;
    }

    public byte[][] KDF_CK(byte[] ck) {
        return null;
    }

    public byte[] ENCRYPT(byte[] mk, String plainText, byte[] associated_data){
        return null;
    }

    public byte[] DECRYPT(byte[] mk, byte[] cipherText, byte[] associated_data) {
        return null;
    }

    public Headers HEADER(KeyPair dhPair, int PN, int N) {
        return null;
    }

    public byte[] CONCAT(byte[] AD, Headers headers) {
        return null;
    }


}
