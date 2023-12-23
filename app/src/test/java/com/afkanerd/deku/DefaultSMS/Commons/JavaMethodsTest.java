package com.afkanerd.deku.DefaultSMS.Commons;

import static org.junit.Assert.assertArrayEquals;

import android.database.Cursor;

import com.google.common.primitives.Bytes;

import org.junit.Test;

public class JavaMethodsTest {

    @Test
    public void byteConcatTest(){
        byte[] byte1 = new byte[]{0x01};
        byte[] byte2 = new byte[]{0x02};

        byte[] expected = new byte[]{byte1[0], byte2[0]};
        byte[] output = Bytes.concat(byte1, byte2);

        assertArrayEquals(expected, output);
    }


}
