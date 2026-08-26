package com.choo.moviefinder.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KoreanRatingGradeTest {

    @Test
    fun `fromApiGradeName maps all four known KMRB grade names correctly`() {
        assertEquals(KoreanRatingGrade.ALL_AGES, KoreanRatingGrade.fromApiGradeName("전체관람가"))
        assertEquals(KoreanRatingGrade.TWELVE_AND_UP, KoreanRatingGrade.fromApiGradeName("12세이상관람가"))
        assertEquals(KoreanRatingGrade.FIFTEEN_AND_UP, KoreanRatingGrade.fromApiGradeName("15세이상관람가"))
        assertEquals(KoreanRatingGrade.RESTRICTED, KoreanRatingGrade.fromApiGradeName("청소년관람불가"))
    }

    @Test
    fun `fromApiGradeName returns null for unknown grade name`() {
        assertNull(KoreanRatingGrade.fromApiGradeName("알수없는등급"))
    }

    @Test
    fun `fromApiGradeName returns null for blank string`() {
        assertNull(KoreanRatingGrade.fromApiGradeName(""))
    }
}
