package com.afkanerd.deku.DefaultSMS.Models.Database;

import java.util.concurrent.Semaphore;

public class SemaphoreManager {

    private static final Semaphore semaphore = new Semaphore(1);

    public static void acquireSemaphore() throws InterruptedException {
        semaphore.acquire();
    }

    public static void releaseSemaphore() throws InterruptedException {
        semaphore.release();
    }
}
