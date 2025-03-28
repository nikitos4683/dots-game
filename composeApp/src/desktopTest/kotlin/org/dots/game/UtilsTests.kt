package org.dots.game

import kotlin.test.Test
import kotlin.test.assertEquals

class UtilsTests {
    @Test
    fun testSplitByUppercase() {
        assertEquals("Multi Word String", splitByUppercase("MultiWordString"))
        assertEquals("multi Word String", splitByUppercase("multiWordString"))
        assertEquals("XYZoo UVW", splitByUppercase("XYZooUVW"))
    }

    @Test
    fun detectInputTypes() {
        checkInputType(InputType.Content, "")
        checkInputType(InputType.Content, "     (;GM[100]FF[4]SZ[20])")

        checkInputType(InputType.File("F:\\Dots\\test.sgf", "test.sgf"), "F:\\Dots\\test.sgf")
        checkInputType(InputType.File("F:\\Dots\\test.sgf", "test.sgf"), "\"F:\\Dots\\test.sgf\"")
        checkInputType(InputType.File("/home/user/docs/test.sgf", "test.sgf"), "/home/user/docs/test.sgf")
        checkInputType(InputType.File("test.sgf", "test.sgf"), "test.sgf")
        checkInputType(InputType.File("path/to/test.md", "test.md", isIncorrect = true), "path/to/test.md")

        checkInputType(InputType.Url("https://zagram.org/eidokropki/backend/download.py?id=zagram377454", "zagram377454"), "https://zagram.org/eidokropki/backend/download.py?id=zagram377454")
        checkInputType(InputType.Url("https://zagram.org/eidokropki/backend/download.py?id=zagram377454", "zagram377454"), "https://zagram.org/eidokropki/index.html#url:zagram377454")
        checkInputType(InputType.Url("https://zagram.org/eidokropki/backend/download.py?id=zagram377454", "zagram377454"), "zagram377454")
        checkInputType(InputType.Url("https://zagram.org/", "", isIncorrect = true), "https://zagram.org/")

        checkInputType(InputType.Other, "test")
    }

    private fun checkInputType(expectedInputType: InputType, text: String) {
        assertEquals(expectedInputType, getInputType(text))
    }
}