package com.hypergonial.chat

import kotlin.test.Test
import kotlin.test.assertEquals

class UtilsTest {

    @Test
    fun testLevEmptyStrings() {
        assertEquals(0, "".levenshteinDistance(""))
    }

    @Test
    fun testLevEmptyAndNonEmpty() {
        assertEquals(4, "".levenshteinDistance("test"))
        assertEquals(4, "test".levenshteinDistance(""))
    }

    @Test
    fun testLevIdenticalStrings() {
        assertEquals(0, "hello".levenshteinDistance("hello"))
        assertEquals(0, "test123".levenshteinDistance("test123"))
    }

    @Test
    fun testLevSingleOperationDifference() {
        assertEquals(1, "test".levenshteinDistance("tests"))
        assertEquals(1, "tests".levenshteinDistance("test"))
        assertEquals(1, "test".levenshteinDistance("tent"))
    }

    @Test
    fun testLevMultipleOperations() {
        assertEquals(3, "kitten".levenshteinDistance("sitting"))
        assertEquals(3, "saturday".levenshteinDistance("sunday"))
    }

    @Test
    fun testLevCaseSensitivity() {
        assertEquals(5, "hello".levenshteinDistance("HELLO"))
    }

    @Test
    fun testLevSpecialCharacters() {
        assertEquals(1, "test!".levenshteinDistance("test?"))
        assertEquals(0, "!@#$%".levenshteinDistance("!@#$%"))
    }

    @Test
    fun testLevLongStrings() {
        val str1 = "abcdefghijklmnopqrstuvwxyz"
        val str2 = "abcdefghijklmnopqrstuvwxyz123"
        assertEquals(3, str1.levenshteinDistance(str2))
    }

    @Test
    fun testLevTranspositions() {
        assertEquals(2, "ab".levenshteinDistance("ba"))
        assertEquals(2, "abcd".levenshteinDistance("acbd"))
    }

    @Test
    fun testLevComplexExamples() {
        assertEquals(5, "intention".levenshteinDistance("execution"))
        assertEquals(3, "algorithm".levenshteinDistance("logarithm"))
        assertEquals(3, "apple".levenshteinDistance("papel"))
    }
}
