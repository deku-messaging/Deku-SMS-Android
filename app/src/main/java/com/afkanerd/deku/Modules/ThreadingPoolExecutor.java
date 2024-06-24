package com.afkanerd.deku.Modules;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadingPoolExecutor {
    public static final ExecutorService executorService = Executors.newFixedThreadPool(4);
}
