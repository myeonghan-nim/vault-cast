package com.example.vaultcast.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class UploadValidationUtilTest {

    @Test
    fun `file extension allows`() {
        assertTrue(UploadValidationUtil.isAllowedExtension("video.mp4"))
        assertTrue(UploadValidationUtil.isAllowedExtension("movie.MKV"))
    }

    @Test
    fun `file extension disallowes`() {
        assertFalse(UploadValidationUtil.isAllowedExtension("document.pdf"))
        assertFalse(UploadValidationUtil.isAllowedExtension("image.jpg"))
    }

    @Test
    fun `input safe`() {
        val safeInput = "This is a safe title"
        assertFalse(UploadValidationUtil.containsDangerousContent(safeInput))
    }

    @Test
    fun `input dangerous`() {
        val dangerousInput = "<script>alert('XSS');</script>"
        assertTrue(UploadValidationUtil.containsDangerousContent(dangerousInput))
    }
}
