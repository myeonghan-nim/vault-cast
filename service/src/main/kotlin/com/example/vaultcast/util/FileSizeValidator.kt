package com.example.vaultcast.util

object FileSizeValidator {
    // 최대 파일 크기: 4GB (바이트)
    private const val MAX_FILE_SIZE = 4L * 1024 * 1024 * 1024

    fun isFileSizeValid(fileSize: Long): Boolean {
        return fileSize in 1..MAX_FILE_SIZE
    }
}
