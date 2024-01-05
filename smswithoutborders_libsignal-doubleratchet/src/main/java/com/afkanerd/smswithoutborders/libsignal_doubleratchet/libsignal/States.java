package com.afkanerd.smswithoutborders.libsignal_doubleratchet.libsignal;

import androidx.annotation.Nullable;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class States {
    public KeyPair DHs;
    public PublicKey DHr;

    public byte[] RK;
    public byte[] CKs;
    public byte[] CKr;

    public int Ns = 0;

    public int Nr = 0;

    public int PN = 0;

    Map<String, String> MKSKIPPED = new HashMap<>();

    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj instanceof States) {
            States state = (States) obj;
            return (
                    (state.DHr != null && this.DHr != null &&
                            Arrays.equals(state.DHr.getEncoded(), this.DHr.getEncoded()))
                    || Objects.equals(state.DHr, this.DHr)) &&
                    state.MKSKIPPED.equals(this.MKSKIPPED) &&
                    state.Ns == this.Ns &&
                    state.Nr == this.Nr &&
                    state.PN == this.PN;
        }
        return false;
    }
}
