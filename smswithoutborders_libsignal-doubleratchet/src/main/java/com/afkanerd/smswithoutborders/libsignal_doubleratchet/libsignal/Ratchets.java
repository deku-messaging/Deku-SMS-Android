package com.afkanerd.smswithoutborders.libsignal_doubleratchet.libsignal;

import android.content.Context;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Arrays;

public class Ratchets {
    public void ratchetInitAlice(String keystoreAlias, States state, byte[] SK,
                                 PublicKey dhPublicKeyBob) throws GeneralSecurityException, IOException, InterruptedException {
        state.DHs = Protocols.GENERATE_DH(keystoreAlias);
        state.DHr = dhPublicKeyBob;
        byte[][] kdfRkOutput = Protocols.KDF_RK(SK,
                Protocols.DH(state.DHs, state.DHr));
        state.RK = kdfRkOutput[0];
        state.CKs = kdfRkOutput[1];
    }

    public void ratchetInitBob(States state, byte[] SK, KeyPair dhKeyPairBob) {
        state.DHs = dhKeyPairBob;
        state.RK = SK;
    }

    public EncryptPayload ratchetEncrypt(States state, byte[] plainText, byte[] AD) throws Throwable {
        byte[][] kdfCkOutput = Protocols.KDF_CK(state.CKs);
        state.CKs = kdfCkOutput[0];
        byte[] mk = kdfCkOutput[1];
        Headers header = Protocols.HEADER(state.DHs, state.PN, state.Ns);
        state.Ns += 1;

        byte[] cipherText = Protocols.ENCRYPT(mk, plainText, Protocols.CONCAT(AD, header));
        return new EncryptPayload(header, cipherText);
    }

    public byte[] ratchetDecrypt(String keystoreAlias, States state, Headers header,
                                 byte[] cipherText, byte[] AD) throws Throwable {
        byte[] plainText = trySkipMessageKeys(header, cipherText, AD);
        if(plainText != null)
            return plainText;

        if(state.DHr == null ||
                !Arrays.equals(header.dh.getEncoded(), state.DHr.getEncoded())) {
            skipMessageKeys(state, header.PN);
            DHRatchet(keystoreAlias, state, header);
        }
        byte[][] kdfCkOutput = Protocols.KDF_CK(state.CKr);
        state.CKr = kdfCkOutput[0];
        byte[] mk = kdfCkOutput[1];
        state.Nr += 1;
        return Protocols.DECRYPT(mk, cipherText, AD);
    }

    private void DHRatchet(String keystoreAlias,
                           States state, Headers header) throws GeneralSecurityException, IOException, InterruptedException {
        state.PN = state.Ns;
        state.Ns = 0;
        state.Nr = 0;
        state.DHr = header.dh;

        byte[][] kdfRkOutput = Protocols.KDF_RK(state.RK, Protocols.DH(state.DHs, state.DHr));
        state.RK = kdfRkOutput[0];
        state.CKr = kdfRkOutput[1];

        state.DHs = Protocols.GENERATE_DH(keystoreAlias);

        kdfRkOutput = Protocols.KDF_RK(state.RK, Protocols.DH(state.DHs, state.DHr));
        state.RK = kdfRkOutput[0];
        state.CKs = kdfRkOutput[1];
    }

    private byte[] trySkipMessageKeys(Headers header, byte[] cipherText, byte[] AD) {
        return null;
    }

    private void skipMessageKeys(States state, int until) {
    }


    public static class EncryptPayload {
        public Headers header;
        public byte[] cipherText;

        public EncryptPayload(Headers header, byte[] cipherText) {
            this.header = header;
            this.cipherText = cipherText;
        }
    }

}
