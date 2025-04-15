package com.example.vaultcast.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "video_metadata")
data class VideoMetadata(
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long = 0,
        val fileName: String = "",
        val title: String?,
        val description: String?,

        // 파일 확장자나 형식 (예: mp4)
        val format: String,

        // 파일 크기 (바이트 단위)
        val size: Long,

        // 업로드 날짜/시간 (자동 생성)
        val uploadDate: LocalDateTime = LocalDateTime.now()
)
