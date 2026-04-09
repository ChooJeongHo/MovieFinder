package com.choo.moviefinder.core.util

import com.choo.moviefinder.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageUrlProviderTest {

    private val baseUrl = BuildConfig.TMDB_IMAGE_BASE_URL

    @Test
    fun `posterUrl - valid path returns full URL with w500`() {
        val result = ImageUrlProvider.posterUrl("/test.jpg")
        assertEquals("${baseUrl}w500/test.jpg", result)
    }

    @Test
    fun `posterUrl - null path returns null`() {
        assertNull(ImageUrlProvider.posterUrl(null))
    }

    @Test
    fun `backdropUrl - valid path returns full URL with w780`() {
        val result = ImageUrlProvider.backdropUrl("/backdrop.jpg")
        assertEquals("${baseUrl}w780/backdrop.jpg", result)
    }

    @Test
    fun `backdropUrl - null path returns null`() {
        assertNull(ImageUrlProvider.backdropUrl(null))
    }

    @Test
    fun `profileUrl - valid path returns full URL with w185`() {
        val result = ImageUrlProvider.profileUrl("/profile.jpg")
        assertEquals("${baseUrl}w185/profile.jpg", result)
    }

    @Test
    fun `profileUrl - null path returns null`() {
        assertNull(ImageUrlProvider.profileUrl(null))
    }

}
