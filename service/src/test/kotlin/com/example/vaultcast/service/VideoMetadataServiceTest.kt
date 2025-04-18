package com.example.vaultcast.service

import com.example.vaultcast.model.VideoMetadata
import com.example.vaultcast.repository.VideoMetadataRepository
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest

@DataJpaTest
class VideoMetadataServiceTest @Autowired constructor(val repository: VideoMetadataRepository) {

    @Test
    fun `metadata save successful`() {
        val meta =
                VideoMetadata(
                        fileName = "test.mp4",
                        title = "T",
                        description = "D",
                        format = "mp4",
                        size = 1234L,
                        uploadDate = LocalDateTime.now()
                )

        val saved = repository.save(meta)
        assertNotNull(saved.id)

        val found = repository.findById(saved.id).orElse(null)
        assertNotNull(found)
        assertEquals("test.mp4", found.fileName)
        assertEquals(1234L, found.size)
    }
}
