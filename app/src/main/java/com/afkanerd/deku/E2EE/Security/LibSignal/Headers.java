package com.afkanerd.deku.E2EE.Security.LibSignal;

import androidx.annotation.Nullable;

import com.google.common.primitives.Bytes;

import org.spongycastle.util.Arrays;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Objects;

public class Headers {

    PublicKey dh;
    public int PN;
    public int N;

    public Headers(KeyPair dhPair, int PN, int N) {
        this.dh = dhPair.getPublic();
        this.PN = PN;
        this.N = N;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj instanceof Headers) {
            Headers header = (Headers) obj;
            return Arrays.areEqual(header.dh.getEncoded(), this.dh.getEncoded()) &&
                    header.PN == this.PN &&
                    header.N == this.N;
        }
        return false;
    }

    public byte[] getSerialized(){
        byte[] values = (PN + "," + N + ",").getBytes();
        return Bytes.concat(values, dh.getEncoded());
    }
}
