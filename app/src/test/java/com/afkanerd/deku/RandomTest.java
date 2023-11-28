package com.afkanerd.deku;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

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
}
