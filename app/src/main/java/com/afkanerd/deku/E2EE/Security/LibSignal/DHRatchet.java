package com.afkanerd.deku.E2EE.Security.LibSignal;


public class DHRatchet {

    private States state;
    public DHRatchet(States state, Headers header){
        this.state = state;
    }

    public States getState(){
        return this.state;
    }
}
