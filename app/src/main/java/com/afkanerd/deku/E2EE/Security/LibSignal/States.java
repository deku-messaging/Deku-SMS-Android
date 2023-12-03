package com.afkanerd.deku.E2EE.Security.LibSignal;

import androidx.annotation.Nullable;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class States {
    public KeyPair DHs;
    public PublicKey DHr;

    public byte[] RK;
    public byte[] CKs;
    public byte[] CKr;

    int Ns = 0;

    int Nr = 0;

    int PN = 0;

    Map<String, String> MKSKIPPED = new HashMap<>();

    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj instanceof States) {
            States state = (States) obj;
            return state.DHr.equals(this.DHr) &&
                    state.MKSKIPPED.equals(this.MKSKIPPED) &&
                    state.Ns == this.Ns &&
                    state.Nr == this.Nr &&
                    state.PN == this.PN;
        }
        return false;
    }
}
