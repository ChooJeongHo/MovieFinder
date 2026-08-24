package com.choo.moviefinder.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TitleMatcherTest {

    @Test
    fun `titleMatches matches titles differing only by whitespace and punctuation`() {
        assertTrue(titleMatches("탑건:매버릭", "탑건: 매버릭"))
    }

    @Test
    fun `titleMatches matches titles differing only by case for English titles`() {
        assertTrue(titleMatches("TOP GUN", "Top Gun"))
    }

    @Test
    fun `titleMatches returns false for partial containment match (conservative matching)`() {
        assertFalse(titleMatches("인터스텔라 리마스터링", "인터스텔라"))
    }

    @Test
    fun `titleMatches returns true for exact match`() {
        assertTrue(titleMatches("테스트 영화", "테스트 영화"))
    }

    @Test
    fun `normalizeTitle strips whitespace, punctuation, and lowercases`() {
        assertEquals("topgun매버릭", normalizeTitle("Top Gun: 매버릭!"))
    }
}
