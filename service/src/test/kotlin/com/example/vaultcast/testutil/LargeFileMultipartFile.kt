package com.example.vaultcast.testutil

import java.io.ByteArrayInputStream
import java.io.InputStream
import org.springframework.mock.web.MockMultipartFile

// 실제 파일 내용은 작게 유지하고 getSize()만 원하는 값을 반환하도록 하는 MockMultipartFile
class LargeFileMultipartFile(
        private val name: String,
        private val originalFilename: String,
        private val contentType: String,
        private val size: Long
) : MockMultipartFile(name, originalFilename, contentType, ByteArray(0)) {

    private val content = ByteArray(0)

    override fun getName(): String = name

    override fun getOriginalFilename(): String = originalFilename

    override fun getContentType(): String? = contentType

    override fun isEmpty(): Boolean = size <= 0

    override fun getSize(): Long = size

    override fun getBytes(): ByteArray = content

    override fun getInputStream(): InputStream = ByteArrayInputStream(content)
}
