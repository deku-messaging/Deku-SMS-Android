package com.afkanerd.deku.E2EE.Security.LibSignal;

import java.security.KeyPair;
import java.security.PublicKey;

public class Ratchets {
    States state;

    public void ratchetInitAlice(States state, byte[] SK, PublicKey dhPublicKeyBob) {
        this.state = state;

        this.state.DHs = Protocols.GENERATE_DH();
        this.state.DHr = dhPublicKeyBob;
        byte[][] kdfRkOutput = Protocols.KDF_RK(SK,
                Protocols.DH(this.state.DHs, this.state.DHr));
        this.state.RK = kdfRkOutput[0];
        this.state.CKs = kdfRkOutput[1];
    }

    public void ratchetInitBob(States state, byte[] SK, KeyPair dhKeyPairBob) {
        this.state = state;

        this.state.DHs = dhKeyPairBob;
        this.state.RK = SK;
    }

    public void ratchetEncrypt(String plainText, byte[] AD) {

    }

    public void ratchetDecrypt(Headers header, String plainText, byte[] AD) {

    }

    private byte[] trySkipMessageKeys(Headers header, byte[] cipherText, byte[] AD) {
        return null;
    }

    private byte[] skipMessageKeys(Headers header, int until) {
        return null;
    }

}
