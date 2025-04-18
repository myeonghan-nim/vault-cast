package com.example.vaultcast.controller

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class StreamingControllerTest(@Autowired val mockMvc: MockMvc) {

    private val streamingUrl = "/v1/stream"
    private val validStreamingUrl = streamingUrl + "/streamTest.mp4"
    private val invalidStreamingUrl = streamingUrl + "/nonStreamTest.mp4"

    private val uploadDir = Paths.get("uploads")
    private val testFile = uploadDir.resolve("streamTest.mp4")

    @BeforeEach
    fun setup() {
        if (!uploadDir.toFile().exists()) Files.createDirectories(uploadDir)
        if (!testFile.toFile().exists()) testFile.toFile().createNewFile()
        Files.write(testFile, "abcdefghijklmnopqrstuvwxyz".toByteArray())
    }

    @AfterEach
    fun cleanup() {
        if (testFile.toFile().exists()) testFile.toFile().delete()
    }

    @Test
    fun `test stream content full successful`() {
        val mvcResult = mockMvc.perform(get(validStreamingUrl)).andExpect(status().isOk).andReturn()

        val body = mvcResult.response.contentAsString
        assertEquals("abcdefghijklmnopqrstuvwxyz", body)
    }

    @Test
    fun `test stream content partial successful`() {
        val mvcResult =
                mockMvc.perform(get(validStreamingUrl).header(HttpHeaders.RANGE, "bytes=5-9"))
                        .andExpect(status().isPartialContent)
                        .andExpect(header().string(HttpHeaders.CONTENT_RANGE, "bytes 5-9/26"))
                        .andReturn()

        val body = mvcResult.response.contentAsString
        assertEquals("fghij", body)
    }

    @Test
    fun `test stream content not found`() {
        mockMvc.perform(get(invalidStreamingUrl)).andExpect(status().isNotFound)
    }

    @Test
    fun `test stream content invalid range`() {
        mockMvc.perform(get(validStreamingUrl).header(HttpHeaders.RANGE, "bytes=10-5"))
                .andExpect(status().isRequestedRangeNotSatisfiable)
    }

    @Test
    fun `test stream content negative range`() {
        mockMvc.perform(get(validStreamingUrl).header(HttpHeaders.RANGE, "bytes=-5--10"))
                .andExpect(status().isRequestedRangeNotSatisfiable)
    }

    @Test
    fun `test stream content range exceeds`() {
        mockMvc.perform(get(validStreamingUrl).header(HttpHeaders.RANGE, "bytes=0-1000"))
                .andExpect(status().isRequestedRangeNotSatisfiable)
    }
}
