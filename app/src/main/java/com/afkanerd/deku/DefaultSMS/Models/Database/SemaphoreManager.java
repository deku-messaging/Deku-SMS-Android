package com.afkanerd.deku.DefaultSMS.Models.Database;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Semaphore;

public class SemaphoreManager {

    private static final Semaphore semaphore = new Semaphore(1);

    private static final Map<Integer, Semaphore> semaphoreMap = new HashMap<>();
    public static void acquireSemaphore(int id) throws InterruptedException {
        if(!semaphoreMap.containsKey(id) || semaphoreMap.get(id) == null)
            semaphoreMap.put(id, new Semaphore(1));

        Objects.requireNonNull(semaphoreMap.get(id)).acquire();
    }

    public static void releaseSemaphore(int id) throws InterruptedException {
        if(!semaphoreMap.containsKey(id) || semaphoreMap.get(id) == null)
            semaphoreMap.put(id, new Semaphore(1));

        Objects.requireNonNull(semaphoreMap.get(id)).release();
    }
    public static void acquireSemaphore() throws InterruptedException {
        semaphore.acquire();
    }

    public static void releaseSemaphore() throws InterruptedException {
        semaphore.release();
    }
}
