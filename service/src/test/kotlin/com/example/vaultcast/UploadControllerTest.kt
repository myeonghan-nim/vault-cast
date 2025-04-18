package com.example.vaultcast

import com.example.vaultcast.repository.VideoMetadataRepository
import com.example.vaultcast.testutil.LargeFileMultipartFile
import java.nio.file.Paths
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
class UploadControllerTest(
        @Autowired val mockMvc: MockMvc,
        @Autowired val metadataRepository: VideoMetadataRepository
) {

    // 테스트에 필요한 변수들
    private val name = "file"
    private val originalFilename = "video.mp4"
    private val contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE
    private val title = "Test Title"
    private val description = "Test Description"

    private val outputDir = Paths.get("uploads").toFile()

    // 테스트 파일
    private val testFile = MockMultipartFile(name, originalFilename, contentType, ByteArray(1))

    // 테스트 후 정리
    @AfterEach
    fun cleanUp() {
        if (outputDir.exists()) {
            outputDir.listFiles()?.forEach { file ->
                if (file.name == originalFilename) file.delete()
            }
        }

        metadataRepository.deleteAll()
    }

    @Test
    fun `test file upload successful`() {
        mockMvc.perform(
                        multipart("/upload")
                                .file(testFile)
                                .param("title", title)
                                .param("description", description)
                )
                .andExpect(status().isCreated)
                .andExpect(content().string(containsString("metadataId")))
                .andExpect(jsonPath("$.fileName").value(originalFilename))
                .andExpect(jsonPath("$.message").value("File uploaded successfully"))

        // 데이터베이스에서 해당 메타데이터가 저장되었는지 직접 확인
        val metadataList = metadataRepository.findAll()
        assertEquals(1, metadataList.size)

        val metadata = metadataList.first()
        assertEquals(originalFilename, metadata.fileName)
        assertEquals(title, metadata.title)
        assertEquals(description, metadata.description)
        assertEquals("mp4", metadata.format)
        assertEquals(testFile.size.toLong(), metadata.size)
    }

    @Test
    fun `test file upload empty file`() {
        val emptyFile = MockMultipartFile(name, originalFilename, contentType, ByteArray(0))

        mockMvc.perform(multipart("/upload").file(emptyFile)).andExpect(status().isBadRequest)
    }

    @Test
    fun `test file upload large file`() {
        val sizeExceeding = 4L * 1024 * 1024 * 1024 + 1
        val largeFile =
                LargeFileMultipartFile(
                        name = name,
                        originalFilename = "big_video.mp4",
                        contentType = contentType,
                        size = sizeExceeding
                )

        mockMvc.perform(
                        multipart("/upload")
                                .file(largeFile)
                                .param("title", title)
                                .param("description", description)
                )
                .andExpect(status().isPayloadTooLarge)
    }

    @Test
    fun `test file upload invalid file type`() {
        val invalidFile = MockMultipartFile(name, "video.exe", contentType, ByteArray(1))

        mockMvc.perform(multipart("/upload").file(invalidFile))
                .andExpect(status().isUnsupportedMediaType)
    }

    @Test
    fun `test file upload dangerous content in metadata`() {
        // 여기서는 제목에 XSS 스크립트를 포함한 경우를 테스트
        mockMvc.perform(
                        multipart("/upload")
                                .file(testFile)
                                .param("title", "<script>alert('XSS')</script>")
                                .param("description", description)
                )
                .andExpect(status().isBadRequest)
    }
}
