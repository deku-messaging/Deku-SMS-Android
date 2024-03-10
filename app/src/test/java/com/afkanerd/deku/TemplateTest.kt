package com.afkanerd.deku

import org.junit.Test

class TemplateTest {
    @Test fun mainTest() {
        println("Hello world")
        val readOnly = "read only stuff"
        var editable = "editable stuff"
        println("Hello, world!")
        editable = "now I edit stuff"
    }
}
