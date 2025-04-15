package com.example.vaultcast.service

import com.example.vaultcast.model.VideoMetadata
import com.example.vaultcast.repository.VideoMetadataRepository
import org.springframework.stereotype.Service

@Service
class VideoMetadataService(private val repository: VideoMetadataRepository) {

    fun saveMetadata(metadata: VideoMetadata): VideoMetadata {
        return repository.save(metadata)
    }

    fun getMetadata(id: Long): VideoMetadata? {
        return repository.findById(id).orElse(null)
    }
}
