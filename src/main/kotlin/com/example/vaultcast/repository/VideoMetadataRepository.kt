package com.example.vaultcast.repository

import com.example.vaultcast.model.VideoMetadata
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository interface VideoMetadataRepository : JpaRepository<VideoMetadata, Long>
