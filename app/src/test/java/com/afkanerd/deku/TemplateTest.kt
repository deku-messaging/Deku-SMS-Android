package com.afkanerd.deku

import org.junit.Test

class TemplateTest {

    object Sample {
        var n = 0
        var v = 0
    }
    @Test fun sampleObjectThreadTest() {
        println(Sample.v)
        Sample.v++
        val t = Thread {
            println(Sample.v)
        }
    }
}
