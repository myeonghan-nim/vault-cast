package com.example.vaultcast.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FileSizeValidatorTest {

    @Test
    fun `file size within limit`() {
        val fileSize = 1L * 1024 * 1024
        assertTrue(FileSizeValidator.isFileSizeValid(fileSize))
    }

    @Test
    fun `file size zero`() {
        assertFalse(FileSizeValidator.isFileSizeValid(0))
    }

    @Test
    fun `file size exceeding limit`() {
        val fileSize = (4L * 1024 * 1024 * 1024) + 1
        assertFalse(FileSizeValidator.isFileSizeValid(fileSize))
    }
}
