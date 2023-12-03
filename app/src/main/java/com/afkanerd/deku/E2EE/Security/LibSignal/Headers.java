package com.afkanerd.deku.E2EE.Security.LibSignal;

import java.security.KeyPair;

public class Headers {

    KeyPair dhPair;
    int PN;
    int N;

    public Headers(KeyPair dhPair, int PN, int N) {
        this.dhPair = dhPair;
        this.PN = PN;
        this.N = N;
    }
}
