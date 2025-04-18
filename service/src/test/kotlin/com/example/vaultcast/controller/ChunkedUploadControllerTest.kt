package com.example.vaultcast

import com.example.vaultcast.repository.VideoMetadataRepository
import com.example.vaultcast.testutil.LargeFileMultipartFile
import java.nio.file.Paths
import kotlin.io.path.readText
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class ChunkedUploadControllerTest(
        @Autowired val mockMvc: MockMvc,
        @Autowired val metadataRepository: VideoMetadataRepository
) {

    private val chunkUploadUrl = "/v1/upload/chunk"
    private val name = "file"
    private val totalChunks = 2
    private val chunkIndex = 0
    private val originalFilename = "video.mp4"
    private val contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE
    private val title = "Test Title"
    private val description = "Test Description"

    private val finalDir = Paths.get("uploads").toFile()
    private val tempDir = Paths.get("uploads", "temp").toFile()

    @AfterEach
    fun cleanUp() {
        if (finalDir.exists()) {
            finalDir.listFiles()?.forEach { file ->
                if (file.name == originalFilename) file.delete()
            }
        }
        if (tempDir.exists()) {
            tempDir.deleteRecursively()
        }

        metadataRepository.deleteAll()
    }

    @Test
    fun `test chunk upload successful`() {
        val fileId = "test-file-partial"

        // 청크 데이터: 첫 번째 청크
        val multipartFile = MockMultipartFile(name, "chunk0", contentType, ByteArray(1))

        mockMvc.perform(
                        multipart(chunkUploadUrl)
                                .file(multipartFile)
                                .param("fileId", fileId)
                                .param("chunkIndex", chunkIndex.toString())
                                .param("totalChunks", totalChunks.toString())
                                .param("originalFileName", originalFilename)
                                .param("title", title)
                                .param("description", description)
                )
                .andExpect(status().isOk)
                .andExpect(
                        content().string(containsString("Chunk $chunkIndex uploaded successfully"))
                )
                .andReturn()
    }

    @Test
    fun `test chunks upload successful`() {
        val fileId = "test-file-complete"

        // 1번째 청크 (index 0): "Hello "
        val chunk0 = MockMultipartFile(name, "chunk0", contentType, "Hello ".toByteArray())
        // 2번째 청크 (index 1): "World!"
        val chunk1 = MockMultipartFile(name, "chunk1", contentType, "World!".toByteArray())

        // 청크 0 업로드
        mockMvc.perform(
                        multipart(chunkUploadUrl)
                                .file(chunk0)
                                .param("fileId", fileId)
                                .param("chunkIndex", "0")
                                .param("totalChunks", totalChunks.toString())
                                .param("originalFileName", originalFilename)
                                .param("title", title)
                                .param("description", description)
                )
                .andExpect(status().isOk)

        // 청크 1 업로드: 마지막 청크로 업로드가 완료되면 202 (Accepted) 상태 반환
        mockMvc.perform(
                        multipart(chunkUploadUrl)
                                .file(chunk1)
                                .param("fileId", fileId)
                                .param("chunkIndex", "1")
                                .param("totalChunks", totalChunks.toString())
                                .param("originalFileName", originalFilename)
                                .param("title", title)
                                .param("description", description)
                )
                .andExpect(status().isAccepted)
                .andExpect(content().string(containsString("File merging in progress.")))

        // 비동기 병합이 완료될 때까지 잠시 대기 (예: 2초)
        Thread.sleep(3000)

        // 최종 파일 (uploads/video.mp4) 확인
        val finalFile = Paths.get("uploads", originalFilename).toFile()
        assert(finalFile.exists()) { "Merged file does not exist" }

        // 최종 파일 내용 확인: 청크의 내용이 올바르게 병합되었는지 확인 (Hello World!)
        val fileContent = finalFile.readText(Charsets.UTF_8)
        assertEquals("Hello World!", fileContent, "Merged file content is incorrect")

        // 데이터베이스에서 해당 메타데이터가 저장되었는지 직접 확인
        val metadataList = metadataRepository.findAll()
        assertEquals(1, metadataList.size)

        val metadata = metadataList.first()
        assertEquals(originalFilename, metadata.fileName)
        assertEquals(title, metadata.title)
        assertEquals(description, metadata.description)
        assertEquals("mp4", metadata.format)
        assertEquals(finalFile.length(), metadata.size)
    }

    @Test
    fun `test chunk upload empty file`() {
        val fileId = "test-file-empty"

        // 빈 청크 데이터
        val multipartFile = MockMultipartFile(name, "chunk0", contentType, ByteArray(0))

        mockMvc.perform(
                        multipart(chunkUploadUrl)
                                .file(multipartFile)
                                .param("fileId", fileId)
                                .param("chunkIndex", chunkIndex.toString())
                                .param("totalChunks", totalChunks.toString())
                                .param("originalFileName", originalFilename)
                                .param("title", title)
                                .param("description", description)
                )
                .andExpect(status().isBadRequest)
                .andExpect(content().string(containsString("Uploaded chunk is empty")))
    }

    @Test
    fun `test chunk upload large file`() {
        val fileId = "test-file-large"

        val sizeExceeding = 4L * 1024 * 1024 * 1024 + 1
        val largeFile =
                LargeFileMultipartFile(
                        name = name,
                        originalFilename = "big_video.mp4",
                        contentType = contentType,
                        size = sizeExceeding
                )

        mockMvc.perform(
                        multipart(chunkUploadUrl)
                                .file(largeFile)
                                .param("fileId", fileId)
                                .param("chunkIndex", chunkIndex.toString())
                                .param("totalChunks", totalChunks.toString())
                                .param("originalFileName", originalFilename)
                                .param("title", title)
                                .param("description", description)
                )
                .andExpect(status().isPayloadTooLarge)
    }

    @Test
    fun `test chunk upload invalid file type`() {
        val fileId = "test-file-invalid"

        // 잘못된 확장자 (예: exe)
        val invalidFile = MockMultipartFile(name, "chunk0", contentType, ByteArray(1))

        mockMvc.perform(
                        multipart(chunkUploadUrl)
                                .file(invalidFile)
                                .param("fileId", fileId)
                                .param("chunkIndex", chunkIndex.toString())
                                .param("totalChunks", totalChunks.toString())
                                .param("originalFileName", "video.exe")
                                .param("title", title)
                                .param("description", description)
                )
                .andExpect(status().isUnsupportedMediaType)
                .andExpect(content().string(containsString("Only video files with extensions")))
    }

    @Test
    fun `test chunk upload dangerous content in metadata`() {
        val fileId = "test-file-dangerous"
        // 제목에 위험한 내용 포함
        val dangerousTitle = "<script>alert('XSS')</script>"

        val multipartFile = MockMultipartFile(name, "chunk0", contentType, ByteArray(1))

        mockMvc.perform(
                        multipart(chunkUploadUrl)
                                .file(multipartFile)
                                .param("fileId", fileId)
                                .param("chunkIndex", chunkIndex.toString())
                                .param("totalChunks", totalChunks.toString())
                                .param("originalFileName", originalFilename)
                                .param("title", dangerousTitle)
                                .param("description", description)
                )
                .andExpect(status().isBadRequest)
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "Title contains disallowed or dangerous content"
                                        )
                                )
                )
    }
}
