package com.afkanerd.deku;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.util.Log;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class RandomTest {

    class RandomClass {
        public int value;
    }

    public void randomClassUpdate(RandomClass randomClass) {
        randomClass.value = 1;
    }

    @Test
    public void ObjectBehaviourTest() {
        RandomClass randomClass = new RandomClass();
        randomClass.value = 0;
        randomClassUpdate(randomClass);
        assertEquals(1, randomClass.value);
    }

    List<Integer> output =new ArrayList<>();

    public void sum(int i) {
        output.add(i);
    }

    public void runSum(int i) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                sum(i);
                try {
                    Thread.sleep(1000L - (i*100L));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    @Test
    public void SyncingThreadsTest() {
        for(int i=0;i<5;++i)
            runSum(i);
        List<Integer> expected = new ArrayList<>();
        expected.add(0);
        expected.add(1);
        expected.add(2);
        expected.add(3);
        expected.add(4);
        assertEquals(expected, output);
    }

}
